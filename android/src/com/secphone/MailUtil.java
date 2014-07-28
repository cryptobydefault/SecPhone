package com.secphone;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.openpgp.PGPPublicKey;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class MailUtil {
	private static final String SPHONE = "sphone";
	
	final String openPgpType = "multipart/encrypted; boundary=BOUNDARY;\r\n\tprotocol=\"application/pgp-encrypted\"";
	
	Session 	session;
	Activity 	activity;
	Handler		handler;
		
	public MailUtil(Activity a, Handler h) {
		this.activity = a;
		this.handler = h;
		
		Authenticator authenticator = new Authenticator() {
			private PasswordAuthentication authentication;
			
			{
				authentication = new PasswordAuthentication(
						activity.getString(R.string.mail_server_username), 
						activity.getString(R.string.mail_server_password));
			}
			
			protected PasswordAuthentication getPasswordAuthentication() {
				return authentication;
			}
		};
		
		Properties props = new Properties();
		props.put("mail.transport.protocol", activity.getString(R.string.mail_transport_protocol));
		props.put("mail.smtp.host", activity.getString(R.string.mail_server));
		props.put("mail.smtp.port", activity.getString(R.string.mail_server_port));
		props.put("mail.smtp.auth", "true");
		
		session = Session.getDefaultInstance(props, authenticator);
	}
	
	void sendEncrypted(String from, String to, String subject, String content) {
		String command = "getPublicKeys";
		
		final String f = from;
		final String t = to;
		final String s = subject;
		final String c = content;
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(int code, String message) {
				Log.d(SPHONE, "postEncrypted() doCallback()");
				
				if (code != 200) {
					Log.w(SPHONE, "code is: " + code);
					Util.simpleAlert(activity, message);
					return;
				}

				PGPPublicKey publicKey = (new CryptoUtil()).parsePublicKey(2, message);
				
				doPost(publicKey, f, t, s, c);
			}
		};
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  to);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_GET, ncb, null, params);
		networkTask.execute(new String());
	}
		
	void doPost(PGPPublicKey publicKey, String from, String to, String subject, String content) {
		Crypto crypto = new Crypto(publicKey);

		String encryptedSubject = new String(crypto.encrypt(true, subject.getBytes()));
		String encryptedContent = new String(crypto.encrypt(true, content.getBytes()));
		
		JSONObject json = new JSONObject();
		try {
			json.put("to", to);
			json.put("subject", encryptedSubject);
			json.put("content", encryptedContent);
		} catch(JSONException e) { Log.w(SPHONE, "JSON exception: " + e); }
		
		String res = json.toString();
		
		String command = "postMessage";
				
		final Activity a = activity;
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(int code, String message) {
				if (code != 200) {
					Log.w(SPHONE, "got error on sending message: " + message);
					
					Util.simpleAlert(a, "Error: " + message);
					
					return;
				} else handler.handleMessage(null);
			}
		};
		
		SharedPreferences sp = activity.getSharedPreferences(activity.getString(R.string.preferences), Context.MODE_PRIVATE);
		String email = sp.getString("email", null);
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  from);
		
		Log.w(SPHONE, "sending: " + res);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_POST, ncb, res, params);
		networkTask.execute(new String());						
	}

	public void sendEncryptedBlocking(String from, String to, String subject, String c)
	throws MessagingException {
		Log.w(SPHONE, "MailUtil.sendEncrypted()");
		Message message = new MimeMessage(session);
		
		message.setFrom(new InternetAddress(from));
		message.addRecipient(RecipientType.TO, new InternetAddress(to));				
		message.setSubject(subject);
		
	    MimeMultipart multiPart = new MimeMultipart();
		
		MimeBodyPart textPart = new MimeBodyPart();
	    textPart.setContent("Version: 1\r\n", "application/pgp-encrypted");
	    multiPart.addBodyPart(textPart);

	    MimeBodyPart htmlPart = new MimeBodyPart();
		
	    // String encrypted = encrypt(to, c);
	    String encrypted = null;
	    
	    htmlPart.setContent(encrypted, "application/octet-stream");
	    multiPart.addBodyPart(htmlPart);
	    
	    message.setContent(multiPart, "multipart/encrypted");

	    // new MailTask(message, true).execute(new String());		
	    
		message.saveChanges();
	    
		String ct = message.getContentType();
    
		Pattern pattern = Pattern.compile("boundary=\"(.*)\"");
		Matcher matcher = pattern.matcher(ct);
		String boundary = null;
		if (matcher.find()) boundary = matcher.group(1);
		else Log.e(SPHONE, "boundary not found");

		String new_ct = "multipart/encrypted; boundary=\"" + boundary 
				+ "\";\r\n\t" + "protocol=\"application/pgp-encrypted\"";

		message.setHeader("Content-Type", new_ct);
		
		Transport.send(message);
	}
	
	// XXX this sends encrypted, not signed...
	public void sendSigned(String from, String to, String subject, String c)
	throws MessagingException {
		Log.w(SPHONE, "MailUtil.sendEncrypted()");
		Message message = new MimeMessage(session);
		
		message.setFrom(new InternetAddress(from));
		message.addRecipient(RecipientType.TO, new InternetAddress(to));				
		message.setSubject(subject);
		
	    MimeMultipart multiPart = new MimeMultipart();
		
		MimeBodyPart textPart = new MimeBodyPart();
	    textPart.setContent("Version: 1\r\n", "application/pgp-encrypted");
	    multiPart.addBodyPart(textPart);

	    MimeBodyPart htmlPart = new MimeBodyPart();
	    
		// String encrypted = encrypt(to, c);
	    String encrypted = null;
	    
	    htmlPart.setContent(encrypted, "application/octet-stream");
	    multiPart.addBodyPart(htmlPart);
	    
	    message.setContent(multiPart, "multipart/encrypted");

	    new MailTask(message, true).execute(new String());		
	}
	
	public void sendAttachment(String from, String to, String subject, byte[] a)
	throws MessagingException {
		Log.w(SPHONE, "MailUtil.sendAttachment()");
		Message message = new MimeMessage(session);
		
		message.setFrom(new InternetAddress(from));
		message.addRecipient(RecipientType.TO, new InternetAddress(to));				
		message.setSubject(subject);
		
	    MimeMultipart multiPart = new MimeMultipart();
		
		MimeBodyPart textPart = new MimeBodyPart();
	    textPart.setContent("see attchment", "text/plain");
	    multiPart.addBodyPart(textPart);

	    MimeBodyPart attachment = new MimeBodyPart();
		DataSource ds = new ByteArrayDataSource(a, "application/octet-stream");
		attachment.setDataHandler(new DataHandler(ds));
		attachment.setFileName("public_key.asc");
	    multiPart.addBodyPart(attachment);
	    
	    message.setContent(multiPart);

	    new MailTask(message).execute(new String());		
	}
	
	public void send(String from, String to, String subject, String c)
	throws MessagingException {
		Log.w(SPHONE, "MailUtil.send()");
		
		Message message = new MimeMessage(session);
		
		message.setFrom(new InternetAddress(from));
		message.addRecipient(RecipientType.TO, new InternetAddress(to));
				
		message.setSubject(subject);
		message.setContent(c, "text/plain");
		
		new MailTask(message).execute(new String());
	}
		
	private class MailTask extends AsyncTask<String, String, String> {
		Message message = null;
		boolean encrypt = false;
		
		public MailTask(Message m) {
			this.message = m;
		}
		
		public MailTask(Message m, boolean e) {
			this.message = m;
			this.encrypt = e;
		}
		
	    protected String doInBackground(String... json) {
			try {
				// rewrite Content-Type header field
				if (encrypt) {
					message.saveChanges();
			    
					String ct = message.getContentType();
			    
					Pattern pattern = Pattern.compile("boundary=\"(.*)\"");
					Matcher matcher = pattern.matcher(ct);
					String boundary = null;
					if (matcher.find()) boundary = matcher.group(1);
					else Log.e(SPHONE, "boundary not found");

					String new_ct = "multipart/encrypted; boundary=\"" + boundary 
							+ "\";\r\n\t" + "protocol=\"application/pgp-encrypted\"";

					message.setHeader("Content-Type", new_ct);
				}

				Transport.send(message);
			} catch(MessagingException e) { Log.w(SPHONE, "messaging exception: " + e.getMessage()); }	    	
	    	
	    	return null;
	    }

	    protected void onPostExecute(String in) { }
	}
}
