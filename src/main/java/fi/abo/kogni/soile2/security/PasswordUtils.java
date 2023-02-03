package fi.abo.kogni.soile2.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Helper functions for Password generation
 * @author Thomas Pfau
 *
 */
public class PasswordUtils {

	/**
	 * Not implemented yet
	 * TODO: Implement encryption/decryption of data based on a given password 
	 * @param key
	 * @param encrypted
	 * @return
	 */
	public static String decrypt(String key, String encrypted)
	{
		return null;
	}
	
	public static SecretKey getKeyFromPassword(String password)
		    throws NoSuchAlgorithmException, InvalidKeySpecException {
		    
		    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		    KeySpec spec = new PBEKeySpec(password.toCharArray());
		    SecretKey secret = new SecretKeySpec(factory.generateSecret(spec)
		        .getEncoded(), "AES");
		    return secret;
		}
	
	public static IvParameterSpec generateIv() {
	    byte[] iv = new byte[16];
	    new SecureRandom().nextBytes(iv);
	    return new IvParameterSpec(iv);
	}
}
