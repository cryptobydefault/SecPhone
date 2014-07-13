package com.secphone.persist;

import java.util.*;
import java.util.regex.*;
import com.secphone.util.Password;

public class Message {
	long			id;
	String			meFrom;
	String			meTo;
	int				meType;
	Date			meTimestamp;
	String			meSubject;
	String			meContent;
	
	public Message() { }

	public Message(String from, String to, int type, String subject, String content) {
		this.meFrom = from;
		this.meTo = to;
		this.meType = type;
		this.meTimestamp = new Date();
		this.meSubject = subject;
		this.meContent = content;
	}

	public void persist() 
	throws PersistException {
		(new Persist()).persist(this);
	}

	public void update() 
	throws PersistException {
		(new Persist()).update(this);
	}

	public static List queryByTo(String to) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("to", to);
		
		return (List) (new Persist()).doNamedQuery("messageByTo", 
							 params, true, 0);			
	}

	public long getId() { return id; }
	public void setId(long id) { this.id = id; }

	public String getMeFrom() { return meFrom; }
	public void setMeFrom(String from) { this.meFrom = from; }

	public String getMeTo() { return meTo; }
	public void setMeTo(String to) { this.meTo = to; }

	public int getMeType() { return meType; }
	public void setMeType(int type) { this.meType = type; }

	public Date getMeTimestamp() { return meTimestamp; }
	public void setMeTimestamp(Date timestamp) { this.meTimestamp = timestamp; }

	public String getMeSubject() { return meSubject; }
	public void setMeSubject(String s) { this.meSubject = s; }

	public String getMeContent() { return meContent; }
	public void setMeContent(String content) { this.meContent = content; }
}
