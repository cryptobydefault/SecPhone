package com.secphone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ReadMessageActivity extends Activity {
	private static final String SPHONE = "sphone";
	
	private String from = null;
	private String subject = null;
	private String content = null;
	private String passphrase = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(SPHONE, "ReadMessageActivity.onCreate()");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_read_message);
		
		Intent intent = getIntent();
		from = intent.getStringExtra("from");
		subject = intent.getStringExtra("subject");
		content = intent.getStringExtra("content");
		
		passphrase = intent.getStringExtra("passphrase");		
		setValues();
	}
	
	void setValues() {	
		((TextView) findViewById(R.id.messageFrom)).setText(from);

		Crypto crypto = CryptoUtil.loadKeysFromFlash(this);
		
		byte[] sClear = crypto.decrypt(true, subject.getBytes(), passphrase);
		if (sClear == null) {
			Util.simpleAlert(this, "error decrypting subject");
			return;
		}	
		((TextView) findViewById(R.id.messageSubject)).setText(new String(sClear));
		
		byte[] cClear = crypto.decrypt(true, content.getBytes(), passphrase);
		if (cClear == null) {
			Util.simpleAlert(this, "error decrypting body");
			return;
		}	
		((TextView) findViewById(R.id.messageBody)).setText(new String(cClear));
	}
/*		
	@Override
	public void onBackPressed() {
		Intent intent = this.getIntent();
		intent.putExtra("passphrase", passphrase);

		setResult(RESULT_OK, intent);
		finish();

		super.onBackPressed();
	}
*/
}
