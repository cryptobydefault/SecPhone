package com.secphone;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.util.Log;

public class ServerApi {
	public final static String SPHONE = "sphone";
	
	Activity activity;
	
	public ServerApi(Activity a) { 
		this.activity = a;
	}

	public interface Callback {
		public void callback(Map<String, Object> params);
	}
	
	public void getStatus(String email, Callback callback) {
		String command = "getAccountStatus";
		
		final Callback cb = callback; 
		
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(HttpResponse response, String message) {
				message = message.trim();
				
				Log.w(SPHONE, "message received: " + message);
				
				int status = -1;
				if (response.getStatusLine().getStatusCode() == 200) status = Integer.parseInt(message);
				
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("status", status);
				
				cb.callback(params);
			}
		};
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  email);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_GET, ncb, null, params);
		networkTask.execute(new String());		
	}
	
	void someCrap() {
	/*
		ServerApi serverApi = new ServerApi(this);
		ServerApi.Callback callback = new ServerApi.Callback() {
			public void callback(Map<String, Object> params) {
				int status = ((Integer) params.get("status")).intValue();
				Log.w(SPHONE, "status: " + status);
				
				if (status == -1) {
					// register new user, send email validation and go to validate email activity
					registerNewUser();
				} else if (status == 0) {
					// they exist in system, but do not have valid email.  just send email validation
					// and go to validate email activity
				}
			}
		};
		serverApi.getStatus("this@tht.com", callback);
	*/		
	}
}