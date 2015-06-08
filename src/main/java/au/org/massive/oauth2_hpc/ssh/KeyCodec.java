package au.org.massive.oauth2_hpc.ssh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.KeyPair;

import org.apache.log4j.Logger;
import org.bouncycastle.openssl.EncryptionException;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;

import au.org.massive.oauth2_hpc.Settings;

/**
 * Some utilities to encode/decode key pairs in PEM format using bouncycastle
 * @author jrigby
 *
 */
public class KeyCodec {
	private static Logger log = Logger.getLogger(Settings.class.getName());
	
	protected KeyCodec() {
		
	}
	
	/**
	 * Reads a file into memory as a byte array
	 * @param file
	 * @return file content as a byte array
	 * @throws IOException
	 */
	protected static byte[] readFile(File file) throws IOException {
		FileInputStream fileInputStream = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fileInputStream.read(data);
		fileInputStream.close();
		return data;
	}
	
	/**
	 * Reads a password from the console. Used when the private key needs to be decrypted but
	 * a passphrase is either not provided or incorrect.
	 * @param prompt
	 * @return
	 * @throws IOException
	 */
	private static String readPassword(String prompt) throws IOException {
		Console console = System.console();
	    if (console == null) {
	        throw new IOException("Couldn't get console instance");
	    }
	    return new String(console.readPassword(prompt+": "));
	}
	
	/**
	 * Decodes a key pair from a byte array
	 * @param data byte array
	 * @param passphrase
	 * @return the key pair
	 */
	public static KeyPair decodePEMKeyPair(byte[] data, String passphrase) {
		try {
			ByteArrayInputStream buf = new ByteArrayInputStream(data);
			InputStreamReader in = new InputStreamReader(buf);
			PEMParser pemParser = new PEMParser(in);
			JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

			Object keyObj = pemParser.readObject();
			KeyPair keyPair;

			if (keyObj instanceof PEMEncryptedKeyPair) {
				try {
					PEMDecryptorProvider decryptionProv = new JcePEMDecryptorProviderBuilder().build(passphrase.toCharArray());
					PEMKeyPair decryptedKeyPair = ((PEMEncryptedKeyPair) keyObj).decryptKeyPair(decryptionProv);
					keyPair = converter.getKeyPair(decryptedKeyPair);
				} catch (NullPointerException e) {
					throw new EncryptionException("passphrase required but not given");
				} finally {
					pemParser.close();
				}
			} else {
				keyPair = converter.getKeyPair((PEMKeyPair) keyObj);
			}
			pemParser.close();
		    return keyPair;
		    
		} catch (EncryptionException e) {
			log.error("Could not decrypt private key!");
			try {
				passphrase = readPassword("Enter passphrase for private key");
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
			return decodePEMKeyPair(data, passphrase);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Decodes a key pair from file
	 * @param file
	 * @param passphrase
	 * @return the key pair
	 * @throws IOException
	 */
	public static KeyPair decodePEMKeyPair(File file, String passphrase) throws IOException {
		log.info("Loading key pair file: "+file.getAbsolutePath());
		return decodePEMKeyPair(readFile(file), passphrase);
	}
	
	/**
	 * Encodes a key pair (doesn't encrypt the private key)
	 * @param keyPair
	 * @return base64 encoded key pair
	 */
	public static String encodePEMKeyPair(KeyPair keyPair) {
		return encode(keyPair);
	}
	
	/**
	 * Encodes an object in PEM format using bouncycastle's {@link JcaPEMWriter}
	 * @param keyObj
	 * @return
	 */
	protected static String encode(Object keyObj) {
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			OutputStreamWriter out = new OutputStreamWriter(buf);
			JcaPEMWriter pemWriter = new JcaPEMWriter(out);
			pemWriter.writeObject(keyObj);
			pemWriter.close();
			out.close();
			String keyString = new String(buf.toByteArray());
			buf.close();
			return keyString;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
