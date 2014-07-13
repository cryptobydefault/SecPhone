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

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.openpgp.PGPPublicKey;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

public class MailUtil {
	private static final String SPHONE = "sphone";
	
	final String openPgpType = "multipart/encrypted; boundary=BOUNDARY;\r\n\tprotocol=\"application/pgp-encrypted\"";
	
	Session session;
	Activity activity;
		
	public MailUtil(Activity a) {
		this.activity = a;
		
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
	
	String encrypt(String email, String in) {
		PGPPublicKey publicKey = (new CryptoUtil()).getPublicKeyBlocking(activity, email);
		if (publicKey == null) {
			Log.w(SPHONE, "no public key: " + email);
			return null;
		}
		
		Crypto crypto = new Crypto(publicKey);		
		return new String(crypto.encrypt(true, in.getBytes()));
	}
	
	public void sendEncrypted(String f, String t, String s, String c) {
		final String from = f;
		final String to = t;
		final String subject = s;
		final String content = c;
		
		new Thread(new Runnable() {
			public void run() {	
				// try {
					// sendEncryptedBlocking(from, to, subject, content);
					postEncryptedBlocking(from, to, subject, content);
				// } catch(MessagingException e) { Log.w(SPHONE, "messaging exception: " + e.getMessage()); }
			}
		}).start();
	}
	
	void postEncryptedBlocking(String from, String to, String subject, String content) {
		String encryptedSubject = encrypt(to, subject);
		String encryptedContent = encrypt(to, content);
		
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
			public void doCallback(HttpResponse response, String message) {
				if (response.getStatusLine().getStatusCode() != 200) {
					Log.w(SPHONE, "got error on sending message: " + message);
					
					Util.simpleAlert(a, "Error: " + message);
					
					return;
				}	
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
		String encrypted = encrypt(to, c);
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
		String encrypted = encrypt(to, c);
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
