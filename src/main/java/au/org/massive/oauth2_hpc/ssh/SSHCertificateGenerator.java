package au.org.massive.oauth2_hpc.ssh;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.security.crypto.codec.Base64;

/**
 * Signs ssh-rsa public keys and produces a certificate
 * 
 * @author jrigby
 *
 */
public class SSHCertificateGenerator {

	private SSHCertificateGenerator() {
		
	}
	
	/**
	 * Allowed certificate types
	 * @author jrigby
	 *
	 */
	public enum SSHCertType {
		SSH_CERT_TYPE_USER(1),
		SSH_CERT_TYPE_HOST(2);
		private final int idx;
		SSHCertType(int idx) {
			this.idx = idx;
		}
		public int getValue() {
			return idx;
		}
	}
	
	/**
	 * Interface for {@link SSHExtensions} and {@link SSHCriticalOptions}
	 * @author jrigby
	 *
	 */
	private interface SSHOptions {
		String getValue();
	}
	
	/**
	 * Comparator to facilitate lexical ordering required by the certificate specification
	 * @author jrigby
	 *
	 */
	private static class SSHOptionsComparator implements Comparator<SSHOptions> {

		@Override
		public int compare(SSHOptions o1, SSHOptions o2) {
			return o1.getValue().compareTo(o2.getValue());
		}
		
	}
	
	/**
	 * Allowed SSH extensions
	 * @author jrigby
	 *
	 */
	public enum SSHExtensions implements SSHOptions {
		PERMIT_X11_FORWARDING("permit-X11-forwarding"),
		PERMIT_AGENT_FORWARDING("permit-agent-forwarding"),
		PERMIT_PORT_FORWARDING("permit-port-forwarding"),
		PERMIT_PTY("permit-pty"),
		PERMIT_USER_RC("permit-user-rc");
		private final String value;
		SSHExtensions(String value) {
			this.value = value;
		}
		public String getValue() {
			return value;
		}
	}
	
	/**
	 * Allowed SSH critical options
	 * @author jrigby
	 *
	 */
	public enum SSHCriticalOptions implements SSHOptions {
		FORCE_COMMAND("force-command"),
		SOURCE_ADDRESS("source-address");
		private final String value;
		SSHCriticalOptions(String value) {
			this.value = value;
		}
		public String getValue() {
			return value;
		}
	}
	
	/**
	 * Signs a public key to produce a certificate
	 * 
	 * @param options certificate signing options
	 * @param caPubKey the public key of the CA
	 * @param caPrivKey the private key of the CA
	 * @return a certificate
	 * @throws InvalidKeyException thrown if the keys are invalid
	 * @throws SignatureException thrown if a signature could not be generated
	 */
	public static String generateSSHCertificate(SSHCertificateOptions options, RSAPublicKey caPubKey, RSAPrivateKey caPrivKey) throws IOException, InvalidKeyException, SignatureException {
		final String header = "ssh-rsa-cert-v01@openssh.com ";
		final String footer = " ssh-authz@"+System.currentTimeMillis();
		
		String cert = new String(Base64
				.encode(signCert(options, caPubKey, caPrivKey)));
		
		StringBuilder sb = new StringBuilder();
		sb.append(header);
		sb.append(cert);
		sb.append(footer);
		return sb.toString();
	}
	
	/**
	 * Produces a certificate byte array. The result is formatted by {@link SSHCertificateGenerator#generateSSHCertificate(SSHCertificateOptions, RSAPublicKey, RSAPrivateKey)}
	 * @param options certificate signing options
	 * @param caPubKey the public key of the CA
	 * @param caPrivKey the private key of the CA
	 * @return certificate as a byte array
	 * @throws InvalidKeyException thrown if the keys are invalid
	 * @throws SignatureException thrown if a signature could not be generated
	 */
	private static byte[] signCert(SSHCertificateOptions options, RSAPublicKey caPubKey, RSAPrivateKey caPrivKey) throws IOException, InvalidKeyException, SignatureException {
		Random rnd;
		try {
			rnd = SecureRandom.getInstance("SHA1PRNG", "SUN");
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new RuntimeException(e);
		}
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(buf);
		byte[] nonce = new byte[32];
		rnd.nextBytes(nonce);
		
		writeValue("ssh-rsa-cert-v01@openssh.com", dos);
		writeValue(nonce, dos);
		writeValue(options.getPubKey().getPublicExponent().toByteArray(), dos);
		writeValue(options.getPubKey().getModulus().toByteArray(), dos);
		writeValue(options.getSerial(), dos);
		writeValue(options.getType().getValue(), dos);
		writeValue(options.getKeyId(), dos);
		writeValue(options.getPrincipals(), dos);
		writeValue(options.getValidAfter(), dos);
		writeValue(options.getValidBefore(), dos);
		writeValue(options.getCriticalOpts(), dos);
		writeValue(options.getExtensions(), dos);
		writeValue("", dos); // Reserved, unused
		writeValue(caPubKey, dos);
		dos.flush();
		
		byte[] dataToSign = buf.toByteArray();
		writeSignature(dataToSign, caPrivKey, dos);
		
		dos.close();
		return buf.toByteArray();
	}
	
	private static void writeValue(byte[] data, DataOutputStream out) throws IOException {
		out.writeInt(data.length);
		if (data.length > 0) {
			out.write(data);
		}
	}
	
	private static void writeValue(String data, DataOutputStream out) throws IOException {
		writeValue(data.getBytes(), out);
	}
	
	private static void writeSignature(byte[] dataToSign, RSAPrivateKey privKey, DataOutputStream out) throws InvalidKeyException, SignatureException, IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out1 = new DataOutputStream(buf);
		try {
			Signature signature = Signature.getInstance("SHA1withRSA", new BouncyCastleProvider());
			signature.initSign(privKey);
			signature.update(dataToSign);
			writeValue("ssh-rsa", out1);
			writeValue(signature.sign(), out1);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		out1.close();
		writeValue(buf.toByteArray(), out);
	}
	
	private static void writeValue(String[] data, DataOutputStream out) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out1 = new DataOutputStream(buf);
		for (String d : data) {
			writeValue(d, out1);
		}
		out1.close();
		writeValue(buf.toByteArray(), out);
	}
	
	private static <T extends SSHOptions> void writeValue(Map<T,String> options, DataOutputStream out) throws IOException {
		// Ensure that options are in lexical order
		TreeMap<T,String> sortedOpts = new TreeMap<T,String>(new SSHOptionsComparator());
		sortedOpts.putAll(options);
		
		String[] data = new String[sortedOpts.size() * 2];
		
		int i = 0;
		for (T option : sortedOpts.keySet()) {
			data[i] = option.getValue();
			data[i+1] = options.get(option);
			i += 2;
		}
		writeValue(data, out);
	}
	
	private static <T extends SSHOptions> void writeValue(Collection<T> options, DataOutputStream out) throws IOException {
		// Ensure that options are in lexical order
		Map<T,String> map = new TreeMap<T,String>(new SSHOptionsComparator());
		for (T option : options) {
			map.put(option, "");
		}
		writeValue(map, out);
	}
	
	private static void writeValue(RSAPublicKey key, DataOutputStream out) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out1 = new DataOutputStream(buf);
		writeValue("ssh-rsa", out1);
		writeValue(key.getPublicExponent().toByteArray(), out1);
		writeValue(key.getModulus().toByteArray(), out1);
		out1.close();
		writeValue(buf.toByteArray(), out);
	}
	
	private static void writeValue(int i, DataOutputStream out) throws IOException {
		out.writeInt(i);
	}
	
	private static void writeValue(long i, DataOutputStream out) throws IOException {
		out.writeLong(i);
	}
}
