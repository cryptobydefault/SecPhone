package com.secphone;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class ValidateEmailActivity extends Activity {
	private static final String SPHONE = "sphone";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_validate_email);
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
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	void validateCode(String code) {
		Log.v(SPHONE, "validateCode()");
		
		String command = "validateEmailCode";
		
		final Activity activity = this;		
		NetworkTask.NetworkCallback ncb = new NetworkTask.NetworkCallback() {
			public void doCallback(int code, String message) {
				if (code != 200) {
					Log.w(SPHONE, "got error on sending validation email: " + message);
					
					Util.simpleAlert(activity, "Error: " + message);
					
					return;
				}	

				setResult(RESULT_OK);
				finish();
			}
		};
		
		SharedPreferences sp = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
		String email = sp.getString("email", null);
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("email",  email);
		params.put("code", code);
		
		NetworkTask networkTask = new NetworkTask(activity, command, NetworkTask.METHOD_GET, ncb, null, params);
		networkTask.execute(new String());						
	}
	
	public void validateEmailSubmit(View v) {
		Log.v(SPHONE, "validateEmailSubmit()");
		
		String code = ((EditText) findViewById(R.id.emailValidationCode)).getText().toString();
		
		if (code == null || code.length() == 0) {
			Util.simpleAlert(this, "Please enter code");
			
			return;
		}
		
		validateCode(code);
	}
}
