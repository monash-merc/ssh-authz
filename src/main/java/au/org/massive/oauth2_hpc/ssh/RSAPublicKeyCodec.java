package au.org.massive.oauth2_hpc.ssh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;

import org.springframework.security.crypto.codec.Base64;

/**
 * Some utilities to encode/decode {@link RSAPublicKey} into/from PEM format or ssh-keygen format
 * @author jrigby
 *
 */
public class RSAPublicKeyCodec extends KeyCodec {
	private RSAPublicKeyCodec() {
		super();
	}
	
	public static byte[] encodeKeyToSSHBase64Format(RSAPublicKey key) throws IOException {
		byte[] keyType = "ssh-rsa".getBytes();
		byte[] modulus = key.getModulus().toByteArray();
		byte[] exponent = key.getPublicExponent().toByteArray();
		
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(buf);
		out.writeInt(keyType.length);
		out.write(keyType);
		out.writeInt(exponent.length);
		out.write(exponent);
		out.writeInt(modulus.length);
		out.write(modulus);
		out.close();
		
		return Base64.encode(buf.toByteArray());
	}
	
	public static RSAPublicKey decodeKeyFromSSHBase64Format(byte[] base64Key) throws InvalidKeyException {
		ByteArrayInputStream buf = new ByteArrayInputStream(Base64.decode(base64Key));
		DataInputStream in = new DataInputStream(buf);
		ArrayList<byte[]> fieldList = new ArrayList<byte[]>(3);
		try {
			try {
				while (true) {
					int fieldLength = in.readInt();
					byte[] fieldData = new byte[fieldLength];
					in.read(fieldData);
					fieldList.add(fieldData);
				}
			} catch (EOFException e) {
				// Do nothing; all data read
			}
			in.close();
			buf.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		// Expect three fields in the encoded key
		if (fieldList.size() != 3) {
			throw new InvalidKeyException();
		}
		byte[][] fieldData = new byte[3][];
		int i = 0;
		for (byte[] data : fieldList) {
			fieldData[i] = data;
			i++;
		}
		
		// Expect ssh-rsa key type
		if (!new String(fieldData[0]).equals("ssh-rsa")) {
			throw new InvalidKeyException();
		}
		
		RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(fieldData[2]), new BigInteger(fieldData[1]));
		try {
			KeyFactory factory = KeyFactory.getInstance("RSA");
			return (RSAPublicKey) factory.generatePublic(spec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static RSAPublicKey decodeKeyFromSSHBase64Format(String key) throws InvalidKeyException {
		if (Base64.isBase64(key.getBytes())) {
			return decodeKeyFromSSHBase64Format(key.getBytes());
		}
		
		String[] keyComponents = key.split("\\s+");
		if (keyComponents.length != 3 && keyComponents.length != 2) {
			throw new InvalidKeyException();
		} else if (!keyComponents[0].equals("ssh-rsa")) {
			throw new InvalidKeyException();
		} else if (Base64.isBase64(keyComponents[1].getBytes())) {
			return decodeKeyFromSSHBase64Format(keyComponents[1].getBytes());
		} else {
			throw new InvalidKeyException();
		}
	}

	public static RSAPublicKey decodeKeyFromSSHBase64Format(File file) throws InvalidKeyException, IOException {
		return decodeKeyFromSSHBase64Format(new String(readFile(file)));
	}
	
	public static RSAPublicKey decodePEMPublicKey(byte[] data) {
		return decodePEMPublicKey(data, null);
	}
	
	public static RSAPublicKey decodePEMPublicKey(byte[] data, String passphrase) {
		return (RSAPublicKey) decodePEMKeyPair(data, passphrase).getPublic();
	}
	
	public static RSAPublicKey decodePEMPublicKey(File file) throws IOException {
		return decodePEMPublicKey(readFile(file));
	}
	
	public static RSAPublicKey decodePEMPublicKey(File file, String passphrase) throws IOException {
		return decodePEMPublicKey(readFile(file), passphrase);
	}
	
	public static String encodePEMPublicKey(RSAPublicKey publicKey) {
		return encode(publicKey);
	}
}
