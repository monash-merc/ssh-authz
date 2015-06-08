package au.org.massive.oauth2_hpc.ssh;

import java.io.File;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;

/**
 * Some utilities to encode/decode {@link RSAPrivateKey} into/from PEM format
 * @author jrigby
 *
 */
public class RSAPrivateKeyCodec extends KeyCodec {
	private RSAPrivateKeyCodec() {
		
	}
	
	public static String encodePEMPrivateKey(RSAPrivateKey privateKey) {
		return encode(privateKey);
	}
	
	public static RSAPrivateKey decodePEMPrivateKey(byte[] data) {
		return decodePEMPrivateKey(data, null);
	}
	
	public static RSAPrivateKey decodePEMPrivateKey(byte[] data, String passphrase) {
		return (RSAPrivateKey) decodePEMKeyPair(data, passphrase).getPrivate();
	}
	
	public static RSAPrivateKey decodePEMPrivateKey(File file) throws IOException {
		return decodePEMPrivateKey(readFile(file));
	}
	
	public static RSAPrivateKey decodePEMPrivateKey(File file, String passphrase) throws IOException {
		return decodePEMPrivateKey(readFile(file), passphrase);
	}
}
