package com.secphone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestTargetHost;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

public class NetworkTask extends AsyncTask<String, Integer, HttpResponse> {
	public static final String SPHONE = "sphone";
	
	public static final int METHOD_GET = 0;
	public static final int METHOD_POST = 1;
	
	String url_base;
	
	Activity activity;
	String command;
	int method;
	NetworkCallback callback;
	String body;
	Map<String, String> params;
	
	public NetworkTask(Activity activity, String command, int method, NetworkCallback callback, String body, Map<String, String> params) {
		url_base = activity.getString(R.string.url_base);
		
		this.command = command;
		this.method = method;
		this.callback = callback;
		this.body = body;
		this.params = params;
	}
	
	public interface NetworkCallback {
		public void doCallback(HttpResponse response, String message);
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
	
	public HttpResponse doBlocking() {
		return doInBackground(new String());
	}
	
	protected HttpResponse doInBackground(String... ignore) { 
		HttpUriRequest request;
		String url =  url_base + command + encodeParams();
		Log.d(SPHONE, "network request: " + url);
		
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

   	 	HttpResponse response = null;
   	 	try {
   	 		response = (new DefaultHttpClient()).execute(request);
   	 	} catch(Exception e) { Log.e(SPHONE, "post exception: " + e); }
   	 	
        return response;
	}
	
	public static String readFromNetwork(InputStream inputStream)
	throws IOException {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    byte[] buffer = new byte[1024];
	    
	    int length = 0;
	    while ((length = inputStream.read(buffer)) != -1) baos.write(buffer, 0, length);
	    
	    return new String(baos.toByteArray());
	}
	
	protected void onPostExecute(HttpResponse response) {
		String message = null;
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			try {
				InputStream is = entity.getContent();
				message = readFromNetwork(is);
			} catch(IOException e) { Log.w(SPHONE, "IO: " + e.getMessage()); }
		}

		// if (response.getStatusLine().getStatusCode() == 200) {
			if (callback != null) callback.doCallback(response, message);
			
			return;
		// }				
		
		// Log.w(SPHONE, "network response (" + response.getStatusLine().getStatusCode() + "): " + message);
	}
}
