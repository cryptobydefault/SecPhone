package com.secphone.util;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.*;
import org.apache.log4j.Logger;

public class Mail {
	static Logger logger = Logger.getLogger(Mail.class);
	
	public static void sendMail(String to, String subject, String body) {    
	   String from = "foo@bar.com";
	   String host = "localhost";

		String PASSWORD = "foo";

	   Properties properties = System.getProperties();
	   properties.setProperty("mail.smtp.host", host);
	   properties.setProperty("mail.smtp.port", "465");
	   properties.setProperty("mail.user", "mailuser");
	   properties.setProperty("mail.password", PASSWORD);

	   Session session = Session.getDefaultInstance(properties);

	   try {
	      MimeMessage message = new MimeMessage(session);
	      message.setFrom(new InternetAddress(from));
	      message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

	      message.setSubject(subject);
	      message.setText(body);

	      Transport.send(message);

			logger.debug("message sent successfully");
	   } catch (MessagingException mex) { logger.warn("error sending: " + mex.getMessage()); }
	}
}
