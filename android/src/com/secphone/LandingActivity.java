package com.secphone;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.secphone.test.CryptoTest;

public class LandingActivity extends Activity {
	private static final String SPHONE = "sphone";
	private static final int VALIDATE_EMAIL_ACTIVITY = 0;
	private static final int GENERATE_KEYS_ACTIVITY = 1;
	
	String email;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(SPHONE, "LandingActivity.onCreate()");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landing);
		
		boolean test = false;
		if (test) {
			CryptoTest cryptoTest = new CryptoTest(this);
			
			return;
		}
		
		SharedPreferences sp = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
		email = sp.getString("email", null);
		
		if (email != null) getEmailStatus();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		} else if (id == R.id.logout) {
			Log.d(SPHONE, "logging out...");
			
			finish();
		}

		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v(SPHONE, "onActivityResult: request: " + resultCode + ", result: " + resultCode);
		
		if (requestCode == VALIDATE_EMAIL_ACTIVITY) {		
			if (resultCode == RESULT_OK) {
				Intent intent = new Intent(this, GenerateKeysActivity.class);
				startActivityForResult(intent, GENERATE_KEYS_ACTIVITY);
			}
		} else if (requestCode == GENERATE_KEYS_ACTIVITY) {
			if (resultCode == RESULT_OK) {
				Intent intent = new Intent(this, HomeActivity.class);
				startActivity(intent);				
			}
		}
	}
	
	void getEmailStatus() {
		Log.d(SPHONE, "getEmailStatus()");
		String command = "getAccountStatus";
		
		final Activity activity = this;		
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(HttpResponse response, String message) {
				Log.d(SPHONE, "getEmailStatus() doCallback()");
				
				if (response.getStatusLine().getStatusCode() != 200) {
					Log.w(SPHONE, "got error on get status: " + message);
				}		
				
				message = message.trim();
				int status = -1;
				if (response.getStatusLine().getStatusCode() == 200) status = Integer.parseInt(message);
				
				Log.d(SPHONE, "status: " + status);
				
				SharedPreferences sp = activity.getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
				Editor edit = sp.edit();
				edit.putString("email", email);
				edit.commit();

				if (status == -1) {			
					registerNewUser();
					return;
				} else if (status == 1) {
					Intent intent = new Intent(activity, GenerateKeysActivity.class);
					startActivityForResult(intent, GENERATE_KEYS_ACTIVITY);
				} else if (status == 2) {
					Intent intent = new Intent(activity, HomeActivity.class);
					startActivity(intent);
				}
			}
		};
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  email);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_GET, ncb, null, params);
		networkTask.execute(new String());		
	}
	
	void registerNewUser() {
		Log.d(SPHONE, "registerNewUser()");
		String command = "register";
		
		final Activity activity = this;		
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(HttpResponse response, String message) {
				if (response.getStatusLine().getStatusCode() != 200) {
					Log.w(SPHONE, "got error on register: " + message);
				}				
				
				sendValidationEmail();
			}
		};
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  email);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_GET, ncb, null, params);
		networkTask.execute(new String());		
	}
	
	void sendValidationEmail() {
		Log.d(SPHONE, "sendValidationEmail()");
		String command = "sendEmailValidation";
		
		final Activity activity = this;		
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(HttpResponse response, String message) {
				if (response.getStatusLine().getStatusCode() != 200) {
					Log.w(SPHONE, "got error on sending validation email: " + message);
				}				
			
				Intent intent = new Intent(activity, ValidateEmailActivity.class);
				startActivityForResult(intent, VALIDATE_EMAIL_ACTIVITY);
			}
		};
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  email);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_GET, ncb, null, params);
		networkTask.execute(new String());				
	}

	public void loginSubmit(View v) {
		Log.v(SPHONE, "loginSubmit()");

		email = ((EditText) findViewById(R.id.LoginEmail)).getText().toString();
		
		if (! Util.validateEmail(email)) {
			Util.simpleAlert(this, "Invalid email address");
			
			return;
		}
		
		((EditText) findViewById(R.id.LoginEmail)).setText("");
		getEmailStatus();
	}
}
