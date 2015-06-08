package au.org.massive.oauth2_hpc.ssh;

import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import au.org.massive.oauth2_hpc.ssh.SSHCertificateGenerator.SSHCertType;
import au.org.massive.oauth2_hpc.ssh.SSHCertificateGenerator.SSHCriticalOptions;
import au.org.massive.oauth2_hpc.ssh.SSHCertificateGenerator.SSHExtensions;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Options for SSH certificate signing
 * @author jrigby
 *
 */
public class SSHCertificateOptions {
	final private RSAPublicKey pubKey;
	final private long serial;
	final private String keyId;
	final private SSHCertType type;
	final private String[] principals;
	final private long validBefore;
	final private long validAfter;
	final private ImmutableMap<SSHCriticalOptions, String> criticalOpts;
	final private ImmutableSet<SSHExtensions> extensions;
	
	/**
	 * Private constructor; object instantiated by the {@link SSHCertificateOptions.Builder}
	 * @param pubKey
	 * @param serial
	 * @param keyId
	 * @param type
	 * @param principals
	 * @param validBefore
	 * @param validAfter
	 * @param criticalOpts
	 * @param extensions
	 */
	private SSHCertificateOptions(RSAPublicKey pubKey, long serial,
			String keyId, SSHCertType type, String[] principals,
			long validBefore, long validAfter,
			Map<SSHCriticalOptions, String> criticalOpts,
			Set<SSHExtensions> extensions) {
		super();
		this.pubKey = pubKey;
		this.serial = serial;
		this.keyId = keyId;
		this.type = type;
		this.principals = principals;
		this.validBefore = validBefore;
		this.validAfter = validAfter;
		this.criticalOpts = ImmutableMap.<SSHCriticalOptions, String>builder().putAll(criticalOpts).build();
		this.extensions = ImmutableSet.<SSHExtensions>builder().addAll(extensions).build();
	}
	
	public RSAPublicKey getPubKey() {
		return pubKey;
	}

	public long getSerial() {
		return serial;
	}

	public String getKeyId() {
		return keyId;
	}

	public SSHCertType getType() {
		return type;
	}

	public String[] getPrincipals() {
		return principals;
	}

	public long getValidBefore() {
		return validBefore;
	}

	public long getValidAfter() {
		return validAfter;
	}

	public Map<SSHCriticalOptions, String> getCriticalOpts() {
		return criticalOpts;
	}

	public Set<SSHExtensions> getExtensions() {
		return extensions;
	}
	
	/**
	 * Builds the SSHCertificateOptions object
	 * @author jrigby
	 *
	 */
	public static class Builder {
		private RSAPublicKey pubKey;
		private long serial;
		private String keyId;
		private SSHCertType type;
		private List<String> principals;
		private long validBefore;
		private long validAfter;
		private Map<SSHCriticalOptions, String> criticalOpts;
		private Set<SSHExtensions> extensions;
		
		private Builder() {
			serial = 0;
			keyId = "";
			type = SSHCertType.SSH_CERT_TYPE_USER;
			principals = new LinkedList<String>();
			validBefore = 0;
			validAfter = 0;
			criticalOpts = new HashMap<SSHCriticalOptions, String>();
			extensions = new HashSet<SSHExtensions>();
		}
		
		public SSHCertificateOptions build() {
			String[] principals = this.principals.toArray(new String[this.principals.size()]);
			return new SSHCertificateOptions(pubKey, serial, keyId, type, principals, validBefore, validAfter, criticalOpts, extensions);
		}
		
		public Builder setPubKey(RSAPublicKey pubKey) {
			this.pubKey = pubKey;
			return this;
		}

		public Builder setSerial(long serial) {
			this.serial = serial;
			return this;
		}

		public Builder setKeyId(String keyId) {
			this.keyId = keyId;
			return this;
		}

		public Builder setType(SSHCertType type) {
			this.type = type;
			return this;
		}

		public Builder addPrincipal(String principal) {
			principals.add(principal);
			return this;
		}
		
		public Builder addPrincipals(Collection<String> principals) {
			this.principals.addAll(principals);
			return this;
		}

		public Builder setValidBefore(long validBefore) {
			this.validBefore = validBefore;
			return this;
		}

		public Builder setValidAfter(long validAfter) {
			this.validAfter = validAfter;
			return this;
		}
		
		/**
		 * Sets the validBefore and validAfter timestamps to be
		 * from now to nDays in the future.
		 * @param nDays
		 * @return
		 */
		public Builder setValidDaysFromNow(int nDays) {
			final long oneDay = 86400;
			long now = System.currentTimeMillis() / 1000;
			setValidAfter(now);
			setValidBefore(now + oneDay * nDays);
			return this;
		}

		public Builder addCriticalOptions(Map<SSHCriticalOptions, String> criticalOpts) {
			this.criticalOpts.putAll(criticalOpts);
			return this;
		}
		
		public Builder addCriticalOption(SSHCriticalOptions option, String value) {
			criticalOpts.put(option, value);
			return this;
		}
		
		public Builder addExtension(SSHExtensions extension) {
			extensions.add(extension);
			return this;
		}

		public Builder addExtensions(Collection<SSHExtensions> extensions) {
			this.extensions.addAll(extensions);
			return this;
		}
		
		/**
		 * Sets the standard {@link SSHExtensions}
		 * @return
		 */
		public Builder setDefaultOptions() {
			criticalOpts = new HashMap<SSHCriticalOptions,String>();
			extensions = new HashSet<SSHExtensions>(Arrays.asList(
					SSHExtensions.PERMIT_X11_FORWARDING,
					SSHExtensions.PERMIT_AGENT_FORWARDING,
					SSHExtensions.PERMIT_PORT_FORWARDING,
					SSHExtensions.PERMIT_PTY,
					SSHExtensions.PERMIT_USER_RC
			));
			return this;
		}
	}
	
	/**
	 * Convenience method to return a new builder object
	 * @return
	 */
	public static Builder builder() {
		return new Builder();
	}
}
