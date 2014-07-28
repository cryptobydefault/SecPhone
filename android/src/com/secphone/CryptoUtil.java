package com.secphone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
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
	
	public CryptoUtil() { }
		
	public static boolean isSupportedAlgorithm(int algorithm) {
		if (algorithm == PGPPublicKey.ELGAMAL_ENCRYPT || algorithm == PGPPublicKey.ELGAMAL_GENERAL
				|| algorithm == PGPPublicKey.RSA_ENCRYPT || algorithm == PGPPublicKey.RSA_GENERAL)
			return true;
		
		return false;
	}
	
	// returns n'th public key
	public PGPPublicKey parsePublicKey(int n, String in) {
		Log.d(SPHONE, "in: " + in);
		try {
			InputStream is = PGPUtil.getDecoderStream(new ByteArrayInputStream(in.getBytes()));
			PGPPublicKeyRing ring = new PGPPublicKeyRing(is, new JcaKeyFingerprintCalculator());
			Iterator it = ring.getPublicKeys();
			
			while (--n > 0) it.next();
			while (it.hasNext()) {
				PGPPublicKey pub = (PGPPublicKey) it.next();
				return pub;
			}
			return ring.getPublicKey();
		} catch(Exception e) { Log.w(SPHONE, "error: " + e.getMessage()); }

		return null;
	}
	
	// returns public key with passed ID
	public PGPPublicKey readPublicKeyFromRing(InputStream inputStream, long id) {
		try {
			InputStream is = PGPUtil.getDecoderStream(inputStream);
			PGPPublicKeyRingCollection pubRingCollection = new PGPPublicKeyRingCollection(is);
			return pubRingCollection.getPublicKey(id);
		} catch(Exception e) { Log.w(SPHONE, "error: " + e.getMessage()); }
		
		return null;
	}
		
	// if master key has a supported algorithm, return that
	// otherwise return first key found with supporting algorithm
	public PGPSecretKey readSecretKeyFromRing(InputStream israw) {
		try {
			InputStream is = PGPUtil.getDecoderStream(israw);
			PGPSecretKeyRing ring = new PGPSecretKeyRing(is, new JcaKeyFingerprintCalculator());

			PGPSecretKey master = ring.getSecretKey();	
			Log.w(SPHONE, "alg: " + master.getPublicKey().getAlgorithm() + ", ID: " + String.format("%x", master.getPublicKey().getKeyID()));
			if (isSupportedAlgorithm(master.getPublicKey().getAlgorithm())) {
				Log.d(SPHONE, "secret id: " + String.format("%x",  master.getPublicKey().getKeyID()));
				return master;
			}
			
			Iterator keys = ring.getSecretKeys();
			while (keys.hasNext()) {
				PGPSecretKey secretKey = (PGPSecretKey) keys.next();
				Log.w(SPHONE, "alg: " + secretKey.getPublicKey().getAlgorithm() + ", ID: " + String.format("%x", secretKey.getPublicKey().getKeyID()));
				
				if (isSupportedAlgorithm(secretKey.getPublicKey().getAlgorithm())) {
					Log.d(SPHONE, "secret id: " + String.format("%x",  secretKey.getPublicKey().getKeyID()));
					return secretKey;
				}
			}
			
			Log.w(SPHONE, "no supported key");
		} catch(Exception e) { Log.w(SPHONE, "Error: " + e.getMessage()); }

		return null;
	}
	
	public byte[] exportPublicKey(Activity activity) {
		SharedPreferences sp = activity.getSharedPreferences(activity.getString(R.string.preferences), Activity.MODE_PRIVATE);
		String keyRing = sp.getString("publicKeyRing", null);
		if (keyRing == null) return null;
		
		return keyRing.getBytes();
	}
	
	public byte[] exportSecretKey(Activity activity) {
		SharedPreferences sp = activity.getSharedPreferences(activity.getString(R.string.preferences), Activity.MODE_PRIVATE);
		String keyRing = sp.getString("secretKeyRing", null);
		if (keyRing == null) return null;
		
		return keyRing.getBytes();
	}
	
	public void saveKeys(Crypto crypto, Activity activity) {
		if (crypto.getSecretKeyRing() == null) {
			Log.w(SPHONE, "no secret key ring");
			return;
		}
		
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ArmoredOutputStream aOut = new ArmoredOutputStream(bOut);

        try {
        	aOut.write(crypto.getSecretKeyRing().getEncoded());
        	aOut.close();
        } catch(IOException e) { 
        	Log.w(SPHONE, "IO exception: " + e.getMessage()); 
        	return;
        }
        byte[] secret = bOut.toByteArray();
        
        bOut = new ByteArrayOutputStream();
        aOut = new ArmoredOutputStream(bOut);
        try {
        	aOut.write(crypto.getPublicKeyRing().getEncoded());
        	aOut.close();
        } catch(IOException e) { 
        	Log.w(SPHONE, "IO exception: " + e.getMessage()); 
        	return;
        }        
        byte[] pub = bOut.toByteArray();
        
        SharedPreferences sp = activity.getSharedPreferences(activity.getString(R.string.preferences), Activity.MODE_PRIVATE);
		Editor edit = sp.edit();
		
		edit.putString("secretKeyRing", new String(secret));
		edit.putString("publicKeyRing", new String(pub));
		edit.commit();
	}
	
	public void loadKeys(Crypto crypto, Activity activity) {
		SharedPreferences sp = activity.getSharedPreferences(activity.getString(R.string.preferences), Activity.MODE_PRIVATE);
		String keyRing = sp.getString("secretKeyRing", null);
		
		if (keyRing == null) {
			Log.w(SPHONE, "no key ring!");
			return;
		}
		
		crypto.setSecretKey(readSecretKeyFromRing(new ByteArrayInputStream(keyRing.getBytes())));
		crypto.setPublicKey(crypto.getSecretKey().getPublicKey());
	}
	
	public void writeKeys(Activity activity, boolean sendSecret, String file) {
		byte[] key;
		
		if (sendSecret) key = exportSecretKey(activity);
		else key = exportPublicKey(activity);	
		
		String command = "writeKeys";
				
		final Activity a = activity;
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(int code, String message) {
				if (code != 200) {
					Log.w(SPHONE, "got error on sending keys: " + message);
					
					Util.simpleAlert(a, "Error: " + message);
					
					return;
				}
			}
		};
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("file",  file);
		
		String keyString = new String(key);
		Log.w(SPHONE, "sending: " + keyString);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_POST, ncb, keyString, params);
		networkTask.execute(new String());						
	}
	
	public void postKeys(Activity activity) {
		byte[] pub = exportPublicKey(activity);	
		
		String command = "updatePublicKeys";
				
		final Activity a = activity;
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(int code, String message) {
				if (code != 200) {
					Log.w(SPHONE, "got error on sending public key: " + message);
					
					Util.simpleAlert(a, "Error: " + message);
					
					return;
				}
			}
		};
		
		SharedPreferences sp = activity.getSharedPreferences(activity.getString(R.string.preferences), Context.MODE_PRIVATE);
		String email = sp.getString("email", null);
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  email);
		
		String pubString = new String(pub);
		Log.w(SPHONE, "sending: " + pubString);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_POST, ncb, pubString, params);
		networkTask.execute(new String());						
	}
}
