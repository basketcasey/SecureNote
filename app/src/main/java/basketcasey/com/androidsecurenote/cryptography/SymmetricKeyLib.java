package basketcasey.com.androidsecurenote.cryptography;

import org.spongycastle.util.encoders.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class SymmetricKeyLib {
	private static final int keyLength = 256;
	private static final int iterationCount = 1000;
	private static final int saltLength = keyLength/8;
	private static final String transform = "AES/GCM/NoPadding";
	
	// Set provider to spongycastle
	private static final Charset UTF8 = Charset.forName("UTF-8");
    static {
        Security.insertProviderAt(
                new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    // Method that generates a salted key from a password, this is one use only
    // as the salt isn't retained.
	public static SecretKey genKey(String passwd) throws NoSuchAlgorithmException, InvalidKeySpecException {
		String password  = "password";
		byte[] salt = Base64.decode(genBase64Salt());
		
		KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt,
		                    iterationCount, keyLength);
		SecretKeyFactory keyFactory = SecretKeyFactory
		                    .getInstance("PBKDF2WithHmacSHA1");
		byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
		SecretKey key = new SecretKeySpec(keyBytes, "AES");
		return key;
	}
	
	// Random salt generator
	// Salt needs to be retained for future use if generating a salted key from a password
    public static String genBase64Salt() {
    	SecureRandom random = new SecureRandom();
    	byte[] salt = new byte[saltLength];
    	random.nextBytes(salt);
    	return new String(Base64.encode(salt), UTF8);
    }
    
    // Debugging method to convert the key to a viewable base64 encoded string
    public static String convertKeyBase64(SecretKey key) {
    	byte[] bytesKey = key.getEncoded();
    	String encodedKey = new String(Base64.encode(bytesKey), UTF8);
    	return encodedKey;
    }
    
	// To generate a consistent key from a user password, the same salt will need to be
	// used for each run.  Also, to check that the password was correct, it will need to
	// be tested by decrypting a known value to ensure it works.
	// The salt is a collection of bytes but it's base64 encoded for easy storage as a String
	public static SecretKey genKeyWithSaltPassword(String password,  String salt)throws NoSuchAlgorithmException, InvalidKeySpecException {
		byte[] bytesSalt = Base64.decode(salt.getBytes(UTF8));
		KeySpec keySpec = new PBEKeySpec(password.toCharArray(), bytesSalt,
                iterationCount, keyLength);
		SecretKeyFactory keyFactory = SecretKeyFactory
		                .getInstance("PBKDF2WithHmacSHA1");
		byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();
		SecretKey key = new SecretKeySpec(keyBytes, "AES");
		return key;
	}

	public static String encryptString(String plaintext, SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
		SecureRandom random = new SecureRandom(); // Used for generating initialization vector
		Cipher cipher = Cipher.getInstance(transform);
		// IV is like salt so same two strings encrypted will be different
		// IV needs to be retained for decryption, its not a secret
		byte[] ivBytes = new byte[cipher.getBlockSize()];
		random.nextBytes(ivBytes);
		IvParameterSpec ivParams = new IvParameterSpec(ivBytes);
		
		// Encrypt the string
		cipher.init(Cipher.ENCRYPT_MODE, key, ivParams);
		byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
		
        // Return a base-64-encoded string containing IV + encrypted input string
        byte[] bytesAll = new byte[ivBytes.length + ciphertext.length];
        System.arraycopy(ivBytes, 0, bytesAll, 0, ivBytes.length);
        System.arraycopy(ciphertext, 0, bytesAll, ivBytes.length, ciphertext.length);
        return new String(Base64.encode(bytesAll), UTF8);
	}
	
	public static String decryptString(String ciphertext, SecretKey key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {
		byte[] bytesEnc = Base64.decode(ciphertext.getBytes(UTF8));
		Cipher cipher = Cipher.getInstance(transform);
		
		// Get the IV from the encrypted data
		byte[] ivBytes = new byte[cipher.getBlockSize()];
		System.arraycopy(bytesEnc, 0, ivBytes, 0, cipher.getBlockSize());
		IvParameterSpec ivParams = new IvParameterSpec(ivBytes);
		
		// Need to create a new byte array of the encrypted message minus the IV
		byte[] bytesToDecrypt = new byte[bytesEnc.length - cipher.getBlockSize()];
		System.arraycopy(bytesEnc, cipher.getBlockSize(), bytesToDecrypt, 0, bytesToDecrypt.length);
		
		// Decrypt the message
		cipher.init(Cipher.DECRYPT_MODE, key, ivParams);
		byte[] bytesPlaintext = cipher.doFinal(bytesToDecrypt);
		return new String(bytesPlaintext, UTF8);
	}	
}