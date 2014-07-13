package com.secphone;

import java.io.*;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import com.google.gson.Gson;

import com.secphone.persist.Account;
import com.secphone.persist.Message;
import com.secphone.persist.PersistException;
import com.secphone.util.Mail;

public class SphoneServlet extends HttpServlet {
	static final long serialVersionUID = 42L;
	Logger logger = Logger.getLogger(SphoneServlet.class);

	private class MessageContainer {
		String to;
		String subject;
		String content;

		MessageContainer() { }
	}
	
	void httpError(HttpServletResponse res, int error_code, String message) {
		try {
			res.setStatus(error_code);
			
			if (message == null) return;
			PrintWriter pw = res.getWriter();
			pw.println(message);
		} catch(IOException e) {
			logger.error("HTTP send error");
		}
	}
	
	String readPostData(HttpServletRequest req) {
		try {
			InputStream is = req.getInputStream();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
	    
			int length = 0;
			while ((length = is.read(buffer)) != -1) baos.write(buffer, 0, length);

			logger.warn("read: " + baos.toByteArray().length);

	    	return new String(baos.toByteArray());
	    } catch(Exception e) { logger.error("failed on reading post data"); }

		return null;
	}
	
	Account getAccount(HttpServletRequest req, HttpServletResponse res) {
		String email = req.getParameter("email");
		if (email == null) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "must pass email address");
			return null;
		}
		
		Account acct = Account.queryByEmail(email);
		if (acct == null) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "email not found");
			return null;
		}	
		
		return acct;
	}
	
	void getAccountStatus(HttpServletRequest req, HttpServletResponse res) 
	throws IOException {
		Account acct = getAccount(req, res);
		if (acct == null) return;
		
		PrintWriter pw = res.getWriter();
		pw.println(acct.getStatus());
	}
	
	void register(HttpServletRequest req, HttpServletResponse res) 
	throws IOException {
		String email = req.getParameter("email");		
		if (email == null) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "must pass email address");
			return;
		}
		
		Account acct = Account.queryByEmail(email);
		if (acct != null) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "email already exists");
			return;
		}

		acct = new Account(email);
		try {
			acct.persist();
		} catch(PersistException e) {
			logger.warn("persist error: " + e.getMessage());
			httpError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "persist error");
			
			return;
		}

		httpError(res, HttpServletResponse.SC_OK, null);
	}
	
	void sendEmailValidation(HttpServletRequest req, HttpServletResponse res) 
	throws IOException {		
		Account acct = getAccount(req, res);
		if (acct == null) return;
		
		String code = "1234";
		Mail.sendMail(acct.getEmail(), "SecPhone Email Validation", "Here is your code: " + code);
		acct.setValidationCode(code);
		try {
			acct.update();
		} catch(PersistException e) {
			logger.warn("persist error: " + e.getMessage());
			httpError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "persist error");
			
			return;
		}
		
		httpError(res, HttpServletResponse.SC_OK, null);	
	}
	
	void validateEmailCode(HttpServletRequest req, HttpServletResponse res) 
	throws IOException {
		Account acct = getAccount(req, res);
		if (acct == null) return;
		
		String code = req.getParameter("code");
		if (code == null) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "need to send code");
			return;
		}
		
		PrintWriter pw = res.getWriter();
		if (code.equals(acct.getValidationCode())) {
			acct.setStatus(Account.STATUS_NO_KEYS);
			try {
				acct.update();
			} catch(PersistException e) {
				logger.warn("persist error: " + e.getMessage());
				httpError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "persist error");
				
				return;
			}

			httpError(res, HttpServletResponse.SC_OK, null);
			return;
		}
		
		httpError(res, HttpServletResponse.SC_BAD_REQUEST, "invalid code");
	}
	
	void updatePublicKeys(HttpServletRequest req, HttpServletResponse res) 
	throws IOException {		
		Account acct = getAccount(req, res);
		if (acct == null) return;
		
		if (acct.getStatus() == Account.STATUS_UNVERIFIED) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "account email unverified");
			
			return;
		}
		
		String keys = readPostData(req);
		if (keys == null) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "no keys passed");
			
			return;
		}
		
		logger.warn("keys: " + keys);
		
		acct.setStatus(Account.STATUS_VALID_KEYS);
		acct.setPublicKeys(keys);
		try {
			acct.update();
		} catch(PersistException e) {
			logger.warn("persist error: " + e.getMessage());
			httpError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "persist error");
			
			return;
		}
		
		httpError(res, HttpServletResponse.SC_OK, null);	
	}
	
	void writeKeys(HttpServletRequest req, HttpServletResponse res) 
	throws IOException {		
		String file = req.getParameter("file");

		String keys = readPostData(req);
		if (keys == null) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "no keys passed");
			
			return;
		}
		
		logger.warn("keys: " + keys);
		
		FileOutputStream out = new FileOutputStream("/tmp/" + file);
		out.write(keys.getBytes());
		out.close();	

		httpError(res, HttpServletResponse.SC_OK, null);	
	}

	void readKeys(HttpServletRequest req, HttpServletResponse res) 
	throws IOException {		
		String file = req.getParameter("file");

		BufferedReader br = new BufferedReader(new FileReader("/tmp/" + file));

		String line;
		PrintWriter pw = res.getWriter();
		while ((line = br.readLine()) != null) {
			pw.println(line);
		}
	}

	void getPublicKeys(HttpServletRequest req, HttpServletResponse res) 
	throws IOException {		
		Account acct = getAccount(req, res);
		if (acct == null) return;
		
		if (acct.getStatus() == Account.STATUS_UNVERIFIED) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "account email unverified");
			
			return;
		}
	
		String pub = acct.getPublicKeys();
		if (pub == null) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "no public key for this account");

			return;
		}

		PrintWriter pw = res.getWriter();
		pw.println(pub);
	}

	void readMessages(HttpServletRequest req, HttpServletResponse res) 
	throws IOException {		
		Account acct = getAccount(req, res);
		if (acct == null) return;
		
		if (acct.getStatus() == Account.STATUS_UNVERIFIED) {
			httpError(res, HttpServletResponse.SC_BAD_REQUEST, "account email unverified");
			
			return;
		}

		StringBuffer buffer = new StringBuffer();
		List messages = Message.queryByTo(acct.getEmail());

		Gson gson = new Gson();
		String json = gson.toJson(messages);
/*
		Iterator it = messages.iterator();
		while (it.hasNext()) {
			Message m = (Message) it.next();
			buffer.append(m.getMeContent());
		}
*/
		PrintWriter pw = res.getWriter();
		pw.println(json);
	}

	void postMessage(HttpServletRequest req, HttpServletResponse res) 
	throws IOException {		
		Account acct = getAccount(req, res);
		if (acct == null) return;

		String json = readPostData(req);
		Gson gson = new Gson();
		MessageContainer mc = gson.fromJson(json, MessageContainer.class);

		Message m = new Message(acct.getEmail(), mc.to, 0, mc.subject, mc.content);

		try {
			m.persist();
		} catch(PersistException e) {
			logger.warn("persist error: " + e.getMessage());
			httpError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "persist error");
			
			return;
		}

		httpError(res, HttpServletResponse.SC_OK, null);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		logger.debug("api doGet");

		res.setContentType("application/json;charset=UTF-8");
	/*
		String authHeader = req.getHeader("Authorization");
		if (authHeader == null) {
			logger.warn("no auth");
			httpError(res, res.SC_UNAUTHORIZED, "api interface requires authorization");
			return;
		}
	*/

		String command = req.getPathInfo();

		if (command == null) {
			logger.warn("no command");
			httpError(res, res.SC_BAD_REQUEST, "no command");

			return;
		}
		
		if (command.equals("/getAccountStatus")) { getAccountStatus(req, res); }
		else if (command.equals("/register")) { register(req, res); }
		else if (command.equals("/sendEmailValidation")) { sendEmailValidation(req, res); }
		else if (command.equals("/validateEmailCode")) { validateEmailCode(req, res); }
		else if (command.equals("/getPublicKeys")) { getPublicKeys(req, res); }
		else if (command.equals("/readKeys")) { readKeys(req, res); }
		else if (command.equals("/readMessages")) { readMessages(req, res); }
		else {
			logger.warn("invalid command");
			httpError(res, res.SC_BAD_REQUEST, "invalid command: " + command);

			return;
		}
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		logger.debug("api doPost");

		res.setContentType("application/json;charset=UTF-8");
		String command = req.getPathInfo();

		if (command == null) {
			logger.warn("no command");
			httpError(res, res.SC_BAD_REQUEST, "no command");

			return;
		}
		
		if (command.equals("/updatePublicKeys")) { updatePublicKeys(req, res); }
		else if (command.equals("/writeKeys")) { writeKeys(req, res); }
		else if (command.equals("/postMessage")) { postMessage(req, res); }
		else {
			logger.warn("invalid command: " + command);
			httpError(res, res.SC_BAD_REQUEST, "invalid command: " + command);

			return;
		}
	}
}
