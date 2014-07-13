package com.secphone;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class HomeActivity extends ListActivity {
	private static final String SPHONE = "sphone";
	String email = null;
	JSONArray array = null;

	@Override
	public void onCreate(Bundle icicle) {
	    super.onCreate(icicle);
	    
	    Log.v(SPHONE, "HomeActivity.onCreate()");
	    
		SharedPreferences sp = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
		email = sp.getString("email", null);
		
		readMessages();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(this, ReadMessageActivity.class);
		
		try {
			JSONObject json = array.getJSONObject(position);
			intent.putExtra("from", json.getString("meFrom"));
			intent.putExtra("subject", json.getString("meSubject"));
			intent.putExtra("content", json.getString("meContent"));
		} catch(JSONException e) { Log.w(SPHONE, "json exception: " + e); }

		startActivity(intent); 
	}
	
	void readMessages() {
		String command = "readMessages";
		
		final Activity activity = this;
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(HttpResponse response, String message) {
				Log.v(SPHONE, "got: " + message);

				if (response.getStatusLine().getStatusCode() != 200) {
					Log.w(SPHONE, "got error on sending message: " + message);
					
					return;
				}	

				try {
					array = new JSONArray(message);
					String[] ret = new String[array.length()];
					for (int i = 0; i < array.length(); i++) {
						JSONObject json = array.getJSONObject(i);
						ret[i] = json.getString("meFrom");
					}
					
				    ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, ret);
				    setListAdapter(adapter);
				} catch(JSONException e) { Log.w(SPHONE, "json exception: " + e); }
			}
		};
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  email);
		
		NetworkTask networkTask = new NetworkTask(this, command, NetworkTask.METHOD_GET, ncb, null, params);
		networkTask.execute(new String());
	}
		
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		} else if (id == R.id.new_message) {
			Intent intent = new Intent(this, NewMessageActivity.class);
			startActivity(intent); 
		} else if (id == R.id.logout) {
			Log.d(SPHONE, "logging out...");
			
			SharedPreferences sp = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
			Editor editor = sp.edit();
			editor.remove("email");
			editor.commit();
			
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
}