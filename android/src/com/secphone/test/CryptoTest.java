package com.secphone.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;

import android.app.Activity;
import android.util.Log;

import com.secphone.Crypto;
import com.secphone.CryptoUtil;
import com.secphone.MailUtil;
import com.secphone.NetworkTask;
import com.secphone.Util;

public class CryptoTest {
	String SECFILE = "keys/sec4.gpg";
	String PUBFILE = "keys/pub4.b64";
	String PASSPHRASE = "something";
	String SPHONE = "sphone";
	Activity activity = null;
	
	public CryptoTest(Activity a) {
		this.activity = a;
		
		testFlow();
	}
	
	void testAscii() {
		try {
			String t = "this is just a test";
			byte[] a = Util.addAsciiArmor(t.getBytes());
			Log.v(SPHONE, "ascii: " + new String(a));
		
			byte[] o = Util.removeAsciiArmor(a);
			Log.v(SPHONE, "size: " + o.length);
			Log.v(SPHONE, "back: " + new String(o));
		} catch(Exception e) { Log.w(SPHONE, "error: " + e); }
	}
	
	void testFlow() {
		Log.v(SPHONE, "creating keys...");
		Crypto crypto1 = new Crypto();
		crypto1.generateKeys(2.0, new Crypto.RSAKeyParams("user1@email.com", "pass1"));
		
		// Crypto crypto2 = new Crypto();
		// crypto2.generateKeys(3.0, new Crypto.RSAKeyParams("user2@email.com", "pass2"));
		
		Log.v(SPHONE, "encrypting...");
		byte[] enc = crypto1.encrypt(false, "test".getBytes());
		Log.v(SPHONE, "decrypting...");
		byte[] dec = crypto1.decrypt(false, enc, "pass1");
		
		Log.v(SPHONE, "out: " + new String(dec));
	}
	
	public void writeData(byte[] data, String file) {
		String command = "writeKeys";
		
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(HttpResponse response, String message) {
				if (response.getStatusLine().getStatusCode() != 200) {
					Log.w(SPHONE, "got error on sending data: " + message);
					
					Util.simpleAlert(activity, "Error: " + message);
					
					return;
				}	
			}
		};
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("file",  file);
		
		String keyString = new String(data);
		Log.w(SPHONE, "sending: " + keyString);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_POST, ncb, keyString, params);
		networkTask.execute(new String());						
	}
	
	String getFile(String file) {
		String command = "readKeys";
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("file",  file);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_GET, null, null, params);
		HttpResponse res = networkTask.doBlocking();
		
		String message = null;
		HttpEntity entity = res.getEntity();
		if (entity != null) {
			try {
				InputStream is = entity.getContent();
				message = NetworkTask.readFromNetwork(is);
			} catch(IOException e) { Log.w(SPHONE, "IO: " + e.getMessage()); }
		}
		
		return message;
	}
	
	byte[] zipMessage(String message) {
		byte[] utf8Bytes = null;
		try {
			utf8Bytes = message.getBytes("UTF-8");
		} catch (Exception e) { Log.w(SPHONE, "get bytes: " + e.getMessage()); }
		
		ByteArrayOutputStream compressedOutput = new ByteArrayOutputStream();
		PGPLiteralDataGenerator literalDataGenerator = new PGPLiteralDataGenerator();
		PGPCompressedDataGenerator compressedDataGenerator =
				new PGPCompressedDataGenerator( CompressionAlgorithmTags.ZIP );
		    
		try {
			OutputStream literalDataOutput =
					literalDataGenerator.open( compressedOutput, PGPLiteralData.BINARY, "some file name",
		            utf8Bytes.length, new Date() );
			
		    compressedDataGenerator.close();
		    // Closeables.closeQuietly( compressedOutput );
		} catch ( Exception e ) {
		    Log.w(SPHONE, "error: " + e);
		}

		return compressedOutput.toByteArray();
	}
		
	void testDecrypt(String in) {
		Crypto crypto = new Crypto();
		CryptoUtil cryptoUtil = new CryptoUtil();
		cryptoUtil.loadKeys(crypto, activity);
		
		try {
			InputStream is = new ByteArrayInputStream(in.getBytes());
			ArmoredInputStream ais = new ArmoredInputStream(is, true);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			int c;
			while ((c = ais.read()) >= 0) { baos.write(c); }
			
			Log.w(SPHONE, "decoding: " + baos.toByteArray().length);
			
			byte[] dec = crypto.decrypt(false, baos.toByteArray(), "drpepper");
		
			Log.v(SPHONE, new String(dec));
		} catch(Exception e) { Log.w(SPHONE, "error: " + e.toString()); }
	}
		
	void encryptDecrypt(PGPSecretKey secretKey, String in) {
		Log.d(SPHONE, "encryptDecrypt()");
		
		Crypto crypto = new Crypto(secretKey);
		
		byte[] enc = crypto.encrypt(false, in.getBytes());
		Log.d(SPHONE, "encrypt done");
		byte[] dec = crypto.decrypt(false, enc, PASSPHRASE);
		
		Log.d(SPHONE, new String(dec));
	}
	
	void encryptDecrypt(String in) {
		Log.d(SPHONE, "encryptDecrypt()");
		
		Crypto crypto = new Crypto();
		(new CryptoUtil()).loadKeys(crypto, activity);
		
		byte[] enc = crypto.encrypt(false, in.getBytes());
		Log.d(SPHONE, "encrypt done");
		byte[] dec = crypto.decrypt(false, enc, PASSPHRASE);
		
		Log.d(SPHONE, new String(dec));
	}

	void twoKeys(Activity activity, PGPSecretKey sKey, PGPPublicKey pKey, String in) {
		Crypto crypto1 = new Crypto(pKey);
		byte[] enc = crypto1.encrypt(false, in.getBytes());
		
		Crypto crypto2 = new Crypto(sKey);
		byte[] dec = crypto2.decrypt(false, enc, PASSPHRASE);
		
		Log.v(SPHONE, new String(dec));
	}
}