package com.secphone;

import com.secphone.persist.*;
import com.secphone.util.*;
import java.util.*;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.MessagingException;
import javax.mail.internet.*;
import javax.activation.*;


public class Main {
	public static void main(String[] args) {
		// System.out.println("hello, world");
		try {
			// createAccount("this@that.com");
			createMessage();
			// listMessages();
		} catch(Exception e) { System.out.println("error: " + e.getMessage()); }
		testMail();
	}

	public static void createMessage()
	throws Exception {
		Message m = new Message("from@from.com", "to@to.com", 0, "some subject", "some content");
		m.persist();
	}

	public static void listMessages()
	throws Exception {
		List list = Message.queryByTo("to@to.com");
		Iterator it = list.iterator();
		while (it.hasNext()) {
			Message m = (Message) it.next();
			System.out.println("from: " + m.getMeFrom());
		}
	}

	public static void createAccount(String email) 
	throws Exception {
		Account acct = new Account(email);
		acct.persist();
	}

	public static void testMail() {
		Mail.sendMail("foo@gmail.com", "a subject", "a body");
	}		
	
	public static void sendMail()
	   {    
	      // Recipient's email ID needs to be mentioned.
	      String to = "foo@gmail.com";

	      // Sender's email ID needs to be mentioned
	      String from = "bar@gmail.com";

	      // Assuming you are sending email from localhost
	      String host = "localhost";

	      // Get system properties
	      Properties properties = System.getProperties();

	      // Setup mail server
	      properties.setProperty("mail.smtp.host", host);

	      // Get the default Session object.
	      Session session = Session.getDefaultInstance(properties);

	      try{
	         // Create a default MimeMessage object.
	         MimeMessage message = new MimeMessage(session);

	         // Set From: header field of the header.
	         message.setFrom(new InternetAddress(from));

	         // Set To: header field of the header.
	         message.addRecipient(javax.mail.Message.RecipientType.TO,
	                                  new InternetAddress(to));

	         // Set Subject: header field
	         message.setSubject("This is the Subject Line!");

	         // Now set the actual message
	         message.setText("This is actual message");

	         // Send message
	         Transport.send(message);
	         System.out.println("Sent message successfully....");
	      }catch (MessagingException mex) {
	         mex.printStackTrace();
	      }
	   }
}
