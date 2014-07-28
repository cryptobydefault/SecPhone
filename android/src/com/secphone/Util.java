package com.secphone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.bcpg.ArmoredOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

public class Util {
	private static final String SPHONE = "sphone";
	
	// XXX validate email syntax
	public static boolean validateEmail(String in) {
		return true;
	}
	
	public static void simpleAlert(Context context, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		
		builder.setTitle(message);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) { }
		});

		AlertDialog dialog = builder.create();
		dialog.show();
	}
		
	public static void goToActivity(Activity from, Class to, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(from);
		
		final Activity f = from;
		final Class t = to;
		
		builder.setTitle(message);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) { 
		       	Intent intent = new Intent(f, t);
	        	f.startActivity(intent);
			}
		});

		AlertDialog dialog = builder.create();
		dialog.show();		
	}
	
	public static byte[] addAsciiArmor(byte[] in) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        ArmoredOutputStream aOut = new ArmoredOutputStream(bOut);

        try {
        	aOut.write(in);
        	aOut.close();
        } catch(IOException e) { 
        	Log.w(SPHONE, "IO exception: " + e.getMessage()); 
        	return null;
        }
        
        return bOut.toByteArray();
	}
	
	public static byte[] removeAsciiArmor(byte[] in) {
		try {
			ArmoredInputStream ais = new ArmoredInputStream(new ByteArrayInputStream(in));
			
			byte[] buffer = new byte[8192];
		    int bytesRead;
		    ByteArrayOutputStream output = new ByteArrayOutputStream();
		    while ((bytesRead = ais.read(buffer)) != -1) {
		        output.write(buffer, 0, bytesRead);
		    }
		    
		    return output.toByteArray();
		} catch(IOException e) { Log.w(SPHONE, "IO exception: " + e); }
		
		return null;
	}
}
