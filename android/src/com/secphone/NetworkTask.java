package com.secphone;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.secphone.NetworkTask.NetworkTaskResponse;

public class NetworkTask extends AsyncTask<String, Integer, NetworkTaskResponse> {
	public static final String SPHONE = "sphone";
	
	public static final int METHOD_GET = 0;
	public static final int METHOD_POST = 1;
	
	int errorCode = -1;
	
	String url_base;
	
	String command;
	int method;
	NetworkCallback callback;
	String body;
	Map<String, String> params;
	
	public class NetworkTaskResponse {
		public int		resultCode;
		public String	body;
		
		public NetworkTaskResponse(int c, String b) {
			this.resultCode = c;
			this.body = b;
		}
	}
		
	public NetworkTask(Context context, String command, int method, NetworkCallback callback, String body, Map<String, String> params) {
		url_base = context.getString(R.string.url_base);
		
		this.command = command;
		this.method = method;
		this.callback = callback;
		this.body = body;
		this.params = params;
	}
	
	public interface NetworkCallback {
		public void doCallback(int code, String message);
	}
	
	String encodeParams() {
		StringBuilder sb = new StringBuilder();
		if (params.size() > 0) sb.append("?");
        for (Map.Entry<?,?> entry : params.entrySet()) {
            if (sb.length() > 1) sb.append("&");
            
            try {
            	sb.append(String.format("%s=%s", URLEncoder.encode(entry.getKey().toString(), "UTF-8"),
            			URLEncoder.encode(entry.getValue().toString(), "UTF-8")));
            } catch(UnsupportedEncodingException e) { Log.w(SPHONE, e.getMessage()); }
        }
        return sb.toString(); 
	}
	
	public NetworkTaskResponse doBlocking() {
		return doInBackground(new String());
	}
	
	protected NetworkTaskResponse doInBackground(String... ignore) { 
		HttpUriRequest request;
		String url =  url_base + command + encodeParams();
		Log.v(SPHONE, "network request: " + url);
		
		if (method == 0) request = new HttpGet(url);
		else {
			request = new HttpPost(url);
			if (body != null) {
				try {
					StringEntity entity = new StringEntity(body);
					((HttpPost) request).setEntity(entity);
				} catch(UnsupportedEncodingException e) { Log.e(SPHONE, "unsupported encoding: " + e); }
			}
		}
		
   	 	request.setHeader("Content-type", "text/xml");

   	 	String content = null;
   	 	try {
   	 		HttpResponse response = (new DefaultHttpClient()).execute(request);
   	 		errorCode = response.getStatusLine().getStatusCode();
   	 		
   	 		ByteArrayOutputStream out = new ByteArrayOutputStream();
   	 		response.getEntity().writeTo(out);
   	 		out.close();
   	 		content = out.toString();
   	 	} catch(Exception e) { Log.e(SPHONE, "post exception: " + e); }
   	 	Log.v(SPHONE, "after request");
   	 	
        return new NetworkTaskResponse(errorCode, content);
	}
		
	protected void onPostExecute(NetworkTaskResponse ntr) {
		Log.v(SPHONE, "onPostExecute()");
		if (callback != null) callback.doCallback(ntr.resultCode, ntr.body);
	}
}
