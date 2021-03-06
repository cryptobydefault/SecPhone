package com.secphone;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class NewMessageActivity extends Activity {
	private static final String SPHONE = "sphone";
	private String email = null;
	
	Handler handler = null;
	Activity activity = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(SPHONE, "NewMessageActivity.onCreate()");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_message);

		SharedPreferences sp = getSharedPreferences(getString(R.string.preferences), MODE_PRIVATE);
		email = sp.getString("email", null);
		
		activity = this;
		handler = new Handler() {
			@Override
			public void handleMessage(Message m) {
				finish();
			}
		};
	}
		
	public void newMessageSubmit(View v) {
		Log.v(SPHONE, "newMessageSubmit()");

		String to = ((EditText) findViewById(R.id.newMessageTo)).getText().toString();
		String subject = ((EditText) findViewById(R.id.newMessageSubject)).getText().toString();
		String body = ((EditText) findViewById(R.id.newMessageBody)).getText().toString();
		
		sendMessage(email, to, subject, body);
	}
	
	void sendMessage(String from, String to, String subject, String body) {
		MailUtil mu = new MailUtil(this, handler);
		mu.sendEncrypted(from, to, subject, body);
		
		// finish();
	}
}
