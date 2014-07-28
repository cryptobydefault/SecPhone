package com.secphone;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class GenerateKeysActivity extends ActionBarActivity {
	private static final String SPHONE = "sphone";
	private ProgressBar mProgress;
	private int mProgressStatus = 0;
	Activity activity = this;
	
	private static final int RANDOM_BYTES = 100;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(SPHONE, "GenerateKeysActivity.onCreate()");
		setContentView(R.layout.activity_generate_keys);
		
		final ImageView image = (ImageView) findViewById(R.id.randomGenerator);
		
		mProgressStatus = 0;
		
		image.setOnTouchListener(new OnTouchListener() {
			byte[] randomBytes = new byte[RANDOM_BYTES];

			@Override
			public boolean onTouch(View arg0, MotionEvent me) {
				// XXX obviously we need to do analysis on this, set bounds, etc.
				int bits = Float.floatToIntBits(me.getRawX() * me.getRawY());
				randomBytes[mProgressStatus] = (byte) (bits & 0xff);
						
				mProgressStatus++;
				mProgress.setProgress(mProgressStatus);
				
				if (mProgressStatus >= RANDOM_BYTES) {
					image.setOnTouchListener(null);

					getPassphrase(randomBytes);
				}
				
				return true;
			}
		});
	
		mProgress = (ProgressBar) findViewById(R.id.randomProgressBar);
		mProgress.setProgress(mProgressStatus);
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

	void getPassphrase(byte[] seed) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("");
		alert.setMessage("Enter Passphrase");

		final EditText input = new EditText(this);
		alert.setView(input);

		final byte[] s = seed;
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Log.v(SPHONE, "onClick()");
				
				final String value = input.getText().toString();
				
				final Handler myHandler = new Handler();
				final ProgressDialog progressDialog = new ProgressDialog(activity);
			    progressDialog.setMessage("Please wait");
			    progressDialog.setTitle("Generating keys...");
			    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			    progressDialog.show();
				
				new Thread(new Runnable() {					
					@Override
					public void run() {
						generateKeys(s, value);

						myHandler.post(new Runnable() {					
							@Override
							public void run() {
								progressDialog.dismiss();//dismiss the dialog
								Toast.makeText(getBaseContext(), "Keys generated", Toast.LENGTH_LONG).show();
							}
						});
						
					}
				}).start();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) { }
		});

		alert.show();
	}
	
	void generateKeys(byte[] seed, String passphrase) {
		Log.v(SPHONE, "generateKeys()");

		Crypto crypto = new Crypto();
		crypto.generateKeys(seed, new Crypto.RSAKeyParams("email@email.com", passphrase));
		
		CryptoUtil cryptoUtil = new CryptoUtil();
		cryptoUtil.saveKeys(crypto, this);
		cryptoUtil.postKeys(this);		
		
		setResult(RESULT_OK);
		finish();
	}
}
