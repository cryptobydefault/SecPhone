package com.secphone;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.secphone.HomeActivity.EmailListItem;

public class EmailArrayAdapter extends ArrayAdapter<EmailListItem> {
	private final Context context;
	private final EmailListItem[] items;
	
	public EmailArrayAdapter(Activity a, EmailListItem[] items) {
		super(a, R.layout.view_message_row, items);
		
		this.context = a;
		this.items = items;
	}
	
	@Override
	  public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    
	    View rowView = inflater.inflate(R.layout.view_message_row, parent, false);
	    TextView fromView = (TextView) rowView.findViewById(R.id.from);
	    fromView.setText(items[position].from);
	    TextView subjectView = (TextView) rowView.findViewById(R.id.subject);
	    subjectView.setText(items[position].subject);
	    
	    return rowView;
	  }
}
