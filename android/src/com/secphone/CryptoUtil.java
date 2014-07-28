package com.secphone;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class CryptoUtil {
	private static final String SPHONE = "sphone";
	
	PGPPublicKeyRing publicKeyRing = null;
	PGPSecretKeyRing secretKeyRing = null;
	
	public CryptoUtil(PGPPublicKeyRing pubRing, PGPSecretKeyRing secRing) {
		this.publicKeyRing = pubRing;
		this.secretKeyRing = secRing;
	}
	
	boolean isEncryptingdAlgorithm(int algorithm) {
		switch(algorithm) {
		case PGPPublicKey.ELGAMAL_ENCRYPT: return true;
		case PGPPublicKey.ELGAMAL_GENERAL: return true;
		case PGPPublicKey.RSA_ENCRYPT: return true;
		case PGPPublicKey.RSA_GENERAL: return true;
		default: return false;
		}
	}

	boolean isSigningAlgorithm(int algorithm) {
		switch(algorithm) {
		case PGPPublicKey.ELGAMAL_GENERAL: return true;
		case PGPPublicKey.RSA_SIGN: return true;
		case PGPPublicKey.RSA_GENERAL: return true;
		default: return false;
		}
	}

	PGPPublicKey extractEncryptingKey() {
		if (publicKeyRing == null) {
			Log.w(SPHONE, "no public key ring");
			return null;
		}
		
		Iterator it = publicKeyRing.getPublicKeys();
		while (it.hasNext()) {
			PGPPublicKey pubKey = (PGPPublicKey) it.next();
			if (isEncryptingdAlgorithm(pubKey.getAlgorithm())) return pubKey;
		}
		
		return null;
	}
	
	PGPSecretKey extractDecryptingKey() {
		if (secretKeyRing == null) {
			Log.w(SPHONE, "no secret key ring");
			return null;
		}
		
		Iterator it = secretKeyRing.getSecretKeys();
		while (it.hasNext()) {
			PGPSecretKey secKey = (PGPSecretKey) it.next();
			if (isEncryptingdAlgorithm(secKey.getPublicKey().getAlgorithm())) return secKey;
		}
		
		return null;		
	}
	
	PGPSecretKey extractSigningKey() {
		if (secretKeyRing == null) {
			Log.w(SPHONE, "no secret key ring");
			return null;
		}
		
		Iterator it = secretKeyRing.getSecretKeys();
		while (it.hasNext()) {
			PGPSecretKey secKey = (PGPSecretKey) it.next();
			if (isSigningAlgorithm(secKey.getPublicKey().getAlgorithm())) return secKey;
		}
		
		return null;		
	}
	
	PGPPublicKey extractVerifyingKey() {
		if (publicKeyRing == null) {
			Log.w(SPHONE, "no public key ring");
			return null;
		}
		
		Iterator it = publicKeyRing.getPublicKeys();
		while (it.hasNext()) {
			PGPPublicKey pubKey = (PGPPublicKey) it.next();
			if (isSigningAlgorithm(pubKey.getAlgorithm())) return pubKey;
		}
		
		return null;		
	}

	public static void backupKeys(Context context, Crypto crypto) {
		try {
			byte[] publicAscii = Util.addAsciiArmor(crypto.publicKeyRing.getEncoded());
			byte[] secretAscii = Util.addAsciiArmor(crypto.secretKeyRing.getEncoded());
		
			storeKeysToFlash(context, publicAscii, secretAscii);
			postPublicKeysToNetwork(context, publicAscii);
		} catch(IOException e) { Log.w(SPHONE, "IO exception: " + e); }
	}
	
	private static void storeKeysToFlash(Context context, byte[] p, byte[] s) {
        SharedPreferences sp = context.getSharedPreferences(context.getString(R.string.preferences), Activity.MODE_PRIVATE);
		Editor edit = sp.edit();
		
		edit.putString("secretKeyRing", new String(s));
		edit.putString("publicKeyRing", new String(p));
		edit.commit();
	}
	
	public static Crypto loadKeysFromFlash(Context context) {
        SharedPreferences sp = context.getSharedPreferences(context.getString(R.string.preferences), Activity.MODE_PRIVATE);

        String secRing = sp.getString("secretKeyRing", null);
        String pubRing = sp.getString("publicKeyRing", null);
        
        try {
        	InputStream is = PGPUtil.getDecoderStream(new ByteArrayInputStream(secRing.getBytes()));
        	PGPSecretKeyRing s = new PGPSecretKeyRing(is, new JcaKeyFingerprintCalculator());
        	
        	InputStream ip = PGPUtil.getDecoderStream(new ByteArrayInputStream(pubRing.getBytes()));
        	PGPPublicKeyRing p = new PGPPublicKeyRing(ip, new JcaKeyFingerprintCalculator());
        	
        	return new Crypto(p, s);
        } 
        catch(IOException e) { Log.w(SPHONE, "IO exception: " + e); }
        catch(PGPException e) { Log.w(SPHONE, "PGP exception: " + e); }

		return null;
	}
	
	private static void postPublicKeysToNetwork(Context context, byte[] p) {
		if (p == null) {
			Log.v(SPHONE, "no public key ring");
			return;
		}
		
		String command = "updatePublicKeys";
		
		final Context c = context;
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(int code, String message) {
				if (code != 200) {
					Log.w(SPHONE, "got error on sending public key: " + message);
					
					Util.simpleAlert(c, "Error: " + message);
					
					return;
				}
			}
		};
		
		SharedPreferences sp = c.getSharedPreferences(c.getString(R.string.preferences), Context.MODE_PRIVATE);
		String email = sp.getString("email", null);
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  email);
		
		String pubString = new String(p);
		Log.w(SPHONE, "sending: " + pubString);
		
		NetworkTask networkTask = new NetworkTask(context, command, NetworkTask.METHOD_POST, ncb, pubString, params);
		networkTask.execute(new String());								
	}
	
	public static PGPPublicKeyRing parsePublicKeyRing(String in) {
		try {
			InputStream is = PGPUtil.getDecoderStream(new ByteArrayInputStream(in.getBytes()));
			return new PGPPublicKeyRing(is, new JcaKeyFingerprintCalculator());
		} catch(IOException e) { 
			Log.w(SPHONE, "IO exception: " + e); 
			return null;
		}
	}
}
