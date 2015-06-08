package au.org.massive.oauth2_hpc;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Represents a client registered to authorise with the OAuth2 server
 * @author jrigby
 *
 */
public class RegisteredClient {
	final String clientName;
	final String clientId;
	final String clientSecret;
	final ImmutableSet<String> allowedGrantTypes;
	final ImmutableSet<String> allowedRedirects;
	
	public RegisteredClient(String clientName, String clientId,
			String clientSecret, Set<String> allowedGrantTypes,
			Set<String> allowedRedirects) {
		super();
		this.clientName = clientName;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.allowedGrantTypes = ImmutableSet.<String>builder().addAll(allowedGrantTypes).build();
		this.allowedRedirects =  ImmutableSet.<String>builder().addAll(allowedRedirects).build();
	}

	public String getClientName() {
		return clientName;
	}

	public String getClientId() {
		return clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public String[] getAllowedGrantTypes() {
		return allowedGrantTypes.toArray(new String[allowedGrantTypes.size()]);
	}

	public String[] getAllowedRedirects() {
		return allowedRedirects.toArray(new String[allowedRedirects.size()]);
	}
	
	
}
