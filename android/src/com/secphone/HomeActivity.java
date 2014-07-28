package com.secphone;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class HomeActivity extends ListActivity {
	private static final String SPHONE = "sphone";
	String email = null;
	JSONArray array = null;
	
	private String passphrase = null;
	
	public class EmailListItem {
		public String from;
		public String subject;
		
		public EmailListItem(String f, String s) {
			this.from = f;
			this.subject = s;
		}
	}

	@Override
	public void onCreate(Bundle icicle) {
	    super.onCreate(icicle);
	    
	    Log.v(SPHONE, "HomeActivity.onCreate()");
	    
		SharedPreferences sp = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
		email = sp.getString("email", null);
		
		new Thread(new Runnable() {
			public void run() {	
				readMessages();
			}
		}).start();
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(this, ReadMessageActivity.class);
		
		try {
			JSONObject json = array.getJSONObject(position);
			intent.putExtra("from", json.getString("meFrom"));
			intent.putExtra("subject", json.getString("meSubject"));
			intent.putExtra("content", json.getString("meContent"));
			if (passphrase != null) intent.putExtra("passphrase", passphrase);
		} catch(JSONException e) { Log.w(SPHONE, "json exception: " + e); }

		startActivity(intent);
	}

	void getPassphrase() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("");
		alert.setMessage("Enter Passphrase");

		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Log.v(SPHONE, "onClick()");
				
				passphrase = input.getText().toString();	
				if (passphrase != null) loadValues();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) { 
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		alert.show();		
	}
	
	void readMessages() {
		String command = "readMessages";
		
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(int code, String message) {
				Log.v(SPHONE, "got: " + message);

				try {
					array = new JSONArray(message);
					Log.v(SPHONE, "messages: " + array.length());
					
					if (array.length() == 0) return;
					
					if (passphrase == null) {
						getPassphrase();
						return;
					}
					
					loadValues();
				} catch(JSONException e) { Log.w(SPHONE, "json exception: " + e); }
			}
		};
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  email);
		
		NetworkTask networkTask = new NetworkTask(this, command, NetworkTask.METHOD_GET, ncb, null, params);
		networkTask.execute(new String());
	}
	
	void loadValues() {
		String[] ret = new String[array.length()];
		EmailListItem[] items = new EmailListItem[array.length()];
/*		
		Crypto crypto = new Crypto();
		CryptoUtil cryptoUtil = new CryptoUtil();
		cryptoUtil.loadKeys(crypto, this);
*/
		Crypto crypto = CryptoUtil.loadKeysFromFlash(this);
		
		try {
			for (int i = 0; i < array.length(); i++) {
				JSONObject json = array.getJSONObject(i);
				ret[i] = json.getString("meFrom");
				
				byte[] sClear = crypto.decrypt(true, json.getString("meSubject").getBytes(), passphrase);
				if (sClear == null) {
					Util.simpleAlert(this, "error decrypting subject");
					return;
				}	

				items[i] = new EmailListItem(json.getString("meFrom"), new String(sClear));
			}
		} catch(JSONException e) { Log.w(SPHONE, "json exception: " + e); }
		
	    // ArrayAdapter<EmailListItem> adapter = new ArrayAdapter<EmailListItem>(activity, android.R.layout.simple_list_item_1, items);
		ArrayAdapter<EmailListItem> adapter = new EmailArrayAdapter(this, items);
	    setListAdapter(adapter);		
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