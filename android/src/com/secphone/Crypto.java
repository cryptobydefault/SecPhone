package com.secphone;

import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;

import javax.crypto.Cipher;

import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.params.RSAKeyGenerationParameters;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPKeyRingGenerator;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.spongycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyConverter;

import android.util.Log;

public class Crypto {
	private static final String SPHONE = "sphone";

	private PGPPublicKey publicKey = null;
	private PGPSecretKey secretKey = null;
	
	private PGPPublicKeyRing publicKeyRing = null;
	private PGPSecretKeyRing secretKeyRing = null;
	
	public static class RSAKeyParams {
		public RSAKeyParams(String i, String p) {
			this.identity = i;
			this.passphrase = p;
		}
		
		String identity;
		String passphrase;
	}
	
	public Crypto() { 
		Security.addProvider(new BouncyCastleProvider());
	}
	
	public Crypto(PGPPublicKey p) {
		Security.addProvider(new BouncyCastleProvider());

		this.publicKey = p;
	}
	
	public Crypto(PGPSecretKey s) {
		Security.addProvider(new BouncyCastleProvider());
		
		this.secretKey = s;
		this.publicKey = s.getPublicKey();
	}
	
	String getCipher(int algorithm) {
		if (algorithm == PGPPublicKey.ELGAMAL_ENCRYPT || algorithm == PGPPublicKey.ELGAMAL_GENERAL) 
			return "ElGamal";
		else if (algorithm == PGPPublicKey.RSA_ENCRYPT || algorithm == PGPPublicKey.RSA_GENERAL) 
			return "RSA";
		
		return null;
	}
		
	public byte[] encrypt(boolean asciiArmor, byte[] in) {
		if (publicKey == null) return null;
		
		try {
			String cipher = getCipher(publicKey.getAlgorithm());
			if (cipher == null) {
				Log.w(SPHONE, "algorithm not supported: " + publicKey.getAlgorithm());
				return null;
			}
			
			JcaPGPKeyConverter converter = new JcaPGPKeyConverter();
			PublicKey k = converter.getPublicKey(publicKey);
		
			Cipher rsaCipher = Cipher.getInstance(cipher, "SC");
			rsaCipher.init(Cipher.ENCRYPT_MODE, k); 
			
			byte[] ret = rsaCipher.doFinal(in);
			
			if (! asciiArmor) return ret;
			
			return Util.addAsciiArmor(ret);
		} catch(Exception e) { Log.w(SPHONE, "encrypt error: " + e.getMessage()); }
		
		return null;
	}
	
	public byte[] decrypt(boolean ascii, byte[] in, String passphrase) {
		if (secretKey == null) return null;
		
		try {
			PGPPrivateKey pk = secretKey.extractPrivateKey(passphrase.toCharArray(), "SC");
		
			JcaPGPKeyConverter converter = new JcaPGPKeyConverter();
			PrivateKey k = converter.getPrivateKey(pk);
			Log.v(SPHONE, "alg: " + k.getAlgorithm() + ", format: " + k.getFormat() + ", class: " + k.getClass());
			
			String cipher = getCipher(publicKey.getAlgorithm());
			if (cipher == null) {
				Log.w(SPHONE, "unsupported cipher: " + publicKey.getAlgorithm());
				return null;
			}
			
			Cipher rsaCipher = Cipher.getInstance(cipher, "SC");
			rsaCipher.init(Cipher.DECRYPT_MODE, k); 
			
			if (ascii) in = Util.removeAsciiArmor(in);
			
			return rsaCipher.doFinal(in);
		} catch(Exception e) { 
			Log.w(SPHONE, "decrypt error: " + e.getMessage());
			return null;
		}
	}

	// See:  spongycastle/pg/src/test/java/org/spongycastle/openpgp/test/BcPGPRSATest.java
	public void generateKeys(byte[] inputSeed, RSAKeyParams params) {
		char[] cPassPhrase = params.passphrase.toCharArray();

		RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();
		try {
			// kpg.init(new RSAKeyGenerationParameters(new BigInteger("10001", 16), SecureRandom.getInstance("SHA1PRNG"), 2048, 80));
			kpg.init(new RSAKeyGenerationParameters(new BigInteger("10001", 16), new SecureRandom(inputSeed), 2048, 80));
			
			AsymmetricCipherKeyPair kpSgn = kpg.generateKeyPair();
			AsymmetricCipherKeyPair kpEnc = kpg.generateKeyPair();
			
			PGPKeyPair sgnKeyPair = new BcPGPKeyPair(PGPPublicKey.RSA_SIGN, kpSgn, new Date());
			PGPKeyPair encKeyPair = new BcPGPKeyPair(PGPPublicKey.RSA_GENERAL, kpEnc, new Date());

			String identity = params.identity;
			
	        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION,
	                encKeyPair, identity, new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1),
	                null, null, new BcPGPContentSignerBuilder(PGPPublicKey.RSA_GENERAL, HashAlgorithmTags.SHA1), 
	                new BcPBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256).build(cPassPhrase));
	        
	        // keyRingGen.addSubKey(sgnKeyPair, null, null);
	        
	        byte[] encodedSecretKeyRing = keyRingGen.generateSecretKeyRing().getEncoded();
	        secretKeyRing = new PGPSecretKeyRing(encodedSecretKeyRing, new BcKeyFingerprintCalculator());
	        
	        byte[] encodedPublicKeyRing = keyRingGen.generatePublicKeyRing().getEncoded();
	        publicKeyRing = new PGPPublicKeyRing(encodedPublicKeyRing, new BcKeyFingerprintCalculator());
    
	        secretKey = secretKeyRing.getSecretKey();
	        publicKey = secretKey.getPublicKey();
		} 
		// catch(NoSuchAlgorithmException e) { Log.w(SPHONE, "unuspported algorithm"); }
		catch(PGPException e) { Log.w(SPHONE, "PGP exception: " + e.getMessage()); }
		catch(IOException e) { Log.w(SPHONE, "IO exception: " + e.getMessage()); }
	}	

	public void setPublicKey(PGPPublicKey p) { this.publicKey = p; }
	public PGPPublicKey getPublicKey() { return publicKey; }
	
	public void setSecretKey(PGPSecretKey p) { this.secretKey = p; }
	public PGPSecretKey getSecretKey() { return secretKey; }	
	
	public void setSecretKeyRing(PGPSecretKeyRing r) { this.secretKeyRing = r; }
	public PGPSecretKeyRing getSecretKeyRing() { return secretKeyRing; }
	
	public void setPublicKeyRing(PGPPublicKeyRing r) { this.publicKeyRing = r; }
	public PGPPublicKeyRing getPublicKeyRing() { return publicKeyRing; }
}
