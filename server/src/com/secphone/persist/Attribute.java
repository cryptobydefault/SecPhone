package com.secphone.persist;

import java.util.*;

public class Attribute {
	long			attributeId;
	long			mkey;
	long			mval;
	String		mopaque;	
	App			app;

	public static final int ATTR_ACTIVE 					= 1; /* default is ACTIVE */
	public static final int ATTR_TAG_WITH_LOCATION		= 2; /* default is DO NOT TAG WITH LOCATION */
	public static final int ATTR_DETECT_MOTION			= 3;
	public static final int ATTR_DETECT_STATIONARY		= 4;
	public static final int ATTR_NOTIFY_ON_DISCOVERY	= 5; /* default is DO NOT NOTIFY ON DISCOVERY */

	public static final int ATTR_TRUE						= 1;
	public static final int ATTR_FALSE 						= 0;

	public Attribute() { }

	public Attribute(long k, long v, String o) {
		this.mkey = k;
		this.mval = v;
		this.mopaque = o;
	}

	public void persist() 
	throws PersistException {
		(new Persist()).persist(this);
	}

	public void update() 
	throws PersistException {
		(new Persist()).update(this);
	}

	public long getAttributeId() { return attributeId; }
	public void setAttributeId(long id) { this.attributeId = id; }

	public App getApp() { return app; }
	public void setApp(App app) { this.app = app; }

	public long getMkey() { return mkey; }
	public void setMkey(long k) { this.mkey = k; }

	public long getMval() { return mval; }
	public void setMval(long v) { this.mval = v; }

	public String getMopaque() { return mopaque; }
	public void setMopaque(String o) { this.mopaque = o; }
}
