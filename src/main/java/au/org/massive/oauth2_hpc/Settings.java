package au.org.massive.oauth2_hpc;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.log4j.Logger;

import au.org.massive.oauth2_hpc.ssh.KeyCodec;
import au.org.massive.oauth2_hpc.ssh.RSAPrivateKeyCodec;
import au.org.massive.oauth2_hpc.ssh.RSAPublicKeyCodec;
import org.mitre.oauth2.model.ClientDetailsEntity.AuthMethod;

/**
 * Class that provides an abstraction from the configuration files, and sensible defaults if
 * parameters are missing
 * @author jrigby
 *
 */
public class Settings {
	
	private static final Logger log = Logger.getLogger(Settings.class.getName());
	private static Settings instance;
	private Configuration config;
	private static KeyPair jwtSigningKeypair;
	
	private Settings() {
		if (config == null) {
			try {
				config = new PropertiesConfiguration("ssh_authz_server.properties");
			} catch (ConfigurationException e) {
				log.warn("Could not load configuration; defaulting to system configuration.", e);
				config = new SystemConfiguration();
			}			
		}
	}
	
	public static Settings getInstance() {
		if (instance == null) {
			instance = new Settings();
		}
		
		return instance;
	}
	
	private String concatList(String key, String delimiter) {
		StringBuilder sb = new StringBuilder();
		for (Object item : config.getList(key)) {
			if (sb.length() > 0) {
				sb.append(delimiter);
			}
			sb.append((String) item);
		}
		return sb.toString();
	}

	public String getCacheFileLocation() {
		return config.getString("cache-file", "ssh-authz-cache.db");
	}

	public long getCacheExpiryHours() {
		return config.getLong("cache-expiry-hours", 24);
	}
	
	public String getTomcatProtocol() { return config.getString("tomcat-protocol", "AJP/1.3"); }
	
	public int getTomcatPort() {
		return config.getInt("tomcat-port", 9000);
	}

	public String getTomcatBindAddress() { return config.getString("tomcat-bind-address", "localhost"); }

	
	public String getLdapProviderUrl() {
		return config.getString("ldap-provider-url");
	}
	
	public String getLdapSecurityPrincipal() {
		return concatList("ldap-security-principal", ",");
	}
	
	public String getLdapSecurityPassword() {
		return config.getString("ldap-security-password");
	}
	
	public String getLdapSearchBaseDn() {
		return concatList("ldap-search-base-dn", ",");
	}
	
	public String getLdapSearchFilter() {
		return concatList("ldap-search-filter", ",");
	}
	
	public Boolean getLdapSearchSubtree() {
		return config.getBoolean("ldap-search-subtree", true);
	}

	// **** Authentication options START ****
	public AuthenticationMode getAuthenticaionMode() {
		String method = config.getString("authentication-method", "http_headers");
		try {
			return AuthenticationMode.getMethod(method);
		} catch (IllegalArgumentException e) {
			AuthenticationMode authenticationMode = AuthenticationMode.HTTP_HEADERS;
			log.warn("Authentication method " + method + " is invalid; using default: " + authenticationMode.name());
			log.warn("Valid choices are: ");
			for (AuthenticationMode m : AuthenticationMode.values()) {
				log.warn(" * " + m.name());
			}
			return authenticationMode;
		}
	}

	public boolean ignoreSecurityWarnings() {
		return config.getBoolean("ignore-security-warnings", false);
	}

	public String getOIDCIssuer() {
		return config.getString("oidc-issuer");
	}

	public String getOIDCClientId() {
		return config.getString("oidc-client-id");
	}

	public String getOIDCClientSecret() {
		return config.getString("oidc-client-secret");
	}

	public String getOIDCRedirectURI() {
		return config.getString("oidc-redirect-uri");
	}

	public AuthMethod getOIDCAuthMethod() {
		return AuthMethod.getByValue(config.getString("oidc-auth-method", "client_secret_basic"));
	}

	public HashSet<String> getOIDCScopes() {
		List<Object> scopes = config.getList("oidc-scopes", Arrays.asList("openid", "email", "offline_access", "profile"));
		HashSet<String> scopeSet = new HashSet<String>();
		for (Object s : scopes) {
			scopeSet.add((String) s);
		}
		return scopeSet;
	}

	public String getUpstreamAuthHeaderName() { return config.getString("upstream-auth-header-name", "mail"); }
	// **** Authentication options END ****
	
	public KeyPair getJWTSigningKeyPair() {
		// Key already loaded? Return it
		if (jwtSigningKeypair != null) {
			log.info("Key pair requested; returning cached key");
			return jwtSigningKeypair;
		}
		
		String keyPairFilePath = config.getString("token-signing-rsa-key-pair");
		String privKeyPassphrase = config.getString("token-signing-rsa-private-key-passphrase");
		if (keyPairFilePath != null) {
			log.info("Key pair requested; loading from file");
			File keyFile = new File(keyPairFilePath);
			try {
				jwtSigningKeypair = KeyCodec.decodePEMKeyPair(keyFile, privKeyPassphrase);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			log.info("Key pair requested; generating in-memory");
			try {
				SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
				keyGen.initialize(2048, random);
				jwtSigningKeypair = keyGen.generateKeyPair();
			} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
				throw new RuntimeException(e);
			}
		}
		
		return jwtSigningKeypair;
	}

	public String getRegisteredClientsConfigFile() {
		return config.getString("registered-clients-file");
	}
	
	public RSAPublicKey getCAPublicKey() {
		String caPublicKeyFilePath = config.getString("ssh-ca-public-key");
		try {
			return RSAPublicKeyCodec.decodeKeyFromSSHBase64Format(new File(caPublicKeyFilePath));
		} catch (InvalidKeyException | IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public RSAPrivateKey getCAPrivateKey() {
		String caPrivateKeyFilePath = config.getString("ssh-ca-private-key");
		String caPrivateKeyPassphrase = config.getString("ssh-ca-private-key-passphrase");
		try {
			return RSAPrivateKeyCodec.decodePEMPrivateKey(new File(caPrivateKeyFilePath), caPrivateKeyPassphrase);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public int getMaxSSHCertValidity() {
		return config.getInt("ssh-cert-max-valid-days", 1);
	}
	
	public Set<RegisteredClient> getRegisteredClients() throws ConfigurationException {
		String file = getRegisteredClientsConfigFile();
		if (file == null) {
			return null;
		}
		
		XMLConfiguration config = new XMLConfiguration();
		config.setFileName(file);
		config.setValidating(false);
		config.setExpressionEngine(new XPathExpressionEngine());
		config.load();
		
		Set<RegisteredClient> registeredClients = new HashSet<RegisteredClient>();
		
		for (HierarchicalConfiguration clientConfiguration : config.configurationsAt("client")) {
			String clientName = clientConfiguration.getString("name");
			String clientId = clientConfiguration.getString("client-id");
			String clientSecret = clientConfiguration.getString("client-secret");
			
			Set<String> allowedGrantTypes = new HashSet<String>();
			SubnodeConfiguration grants = clientConfiguration.configurationAt("allowed-grant-types");
			for (Object o : grants.getList("grant")) {
				allowedGrantTypes.add((String) o);
			}
			
			Set<String> allowedRedirects = new HashSet<String>();
			SubnodeConfiguration redirects = clientConfiguration.configurationAt("allowed-redirects");
			for (Object o : redirects.getList("url")) {
				allowedRedirects.add((String) o);
			}
			
			registeredClients.add(new RegisteredClient(clientName, clientId, clientSecret, allowedGrantTypes, allowedRedirects));
		}
		
		return registeredClients;
	}
}
