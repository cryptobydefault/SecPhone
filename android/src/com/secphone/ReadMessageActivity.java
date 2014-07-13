package com.secphone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ReadMessageActivity extends Activity {
	private static final String SPHONE = "sphone";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v(SPHONE, "LandingActivity.onCreate()");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_read_message);
		
		Intent intent = getIntent();
		String from = intent.getStringExtra("from");
		String subject = intent.getStringExtra("subject");
		String content = intent.getStringExtra("content");
		
		Crypto crypto = new Crypto();
		CryptoUtil cryptoUtil = new CryptoUtil();
		cryptoUtil.loadKeys(crypto, this);
		
		byte[] sClear = crypto.decrypt(true, subject.getBytes(), "pass");
		
		((TextView) findViewById(R.id.messageFrom)).setText(from);
		((TextView) findViewById(R.id.messageSubject)).setText(new String(sClear));
	}
}
