package com.secphone.util;

import java.security.*;
import java.util.Arrays;

public class Password {
	final static int SALT_LENGTH = 128;
	static SecureRandom secureRandom = new SecureRandom();

	byte[] salt = new byte[SALT_LENGTH];
	byte[] hash;

	static byte[] generateHash(byte[] salt, String password) {
		try {
			byte[] c = new byte[salt.length + password.getBytes("UTF-8").length]; 
			System.arraycopy(salt, 0, c, 0, salt.length); 
			System.arraycopy(password.getBytes("UTF-8"), 0, c, salt.length, password.getBytes("UTF-8").length);

			MessageDigest digest = MessageDigest.getInstance("SHA-256"); 
			return digest.digest(c);
		} catch(Exception e) { System.out.println("error: " + e); }

		return null;
	}

	public Password(String password) {
		secureRandom.nextBytes(this.salt);
		hash = generateHash(salt, password);
	}

	public static boolean validatePassword(String password, byte[] salt, byte[] hash) {
		byte[] compare = generateHash(salt, password);

		return Arrays.equals(compare, hash);
	}

	public byte[] getSalt() { return salt; }
	public byte[] getHash() { return hash; }
}
