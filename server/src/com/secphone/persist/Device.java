package com.secphone.persist;

import java.util.*;

public class Device {
	long			deviceId;
	App			app;
	String		MAC;
	boolean		hasLocation;
	double		latitude;
	double		longitude;

	public Device() { }

	public Device(String MAC) {
		this.MAC = MAC;
	}
	
	public Device(String mac, boolean hasLocation, double lat, double lon) {
		this.MAC = mac;
		this.hasLocation = hasLocation;
		this.latitude = lat;
		this.longitude = lon;
	}

	public void persist() 
	throws PersistException {
		(new Persist()).persist(this);
	}

	public void update() 
	throws PersistException {
		(new Persist()).update(this);
	}

	// XXX this could return multiple records (device registerd by multiple apps)
	public static Device queryByMAC(String m) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("MAC", m);
		
		return (Device) (new Persist()).doNamedQuery("deviceByMAC", 
							 params, false, 0);			
	}
	
	public static Device queryByMacAndApp(String mac, App app) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("MAC", mac);
		params.put("app", app);
		
		return (Device) (new Persist()).doNamedQuery("deviceByMacAndApp", 
							 params, false, 0);					
	}

	public long getDeviceId() { return deviceId; }
	public void setDeviceId(long id) { this.deviceId = id; }

	public App getApp() { return app; }
	public void setApp(App app) { this.app = app; }

	public String getMAC() { return MAC; }
	public void setMAC(String MAC) { this.MAC = MAC; }

	public boolean getHasLocation() { return hasLocation; }
	public void setHasLocation(boolean h) { this.hasLocation = h; }

	public double getLatitude() { return latitude; }
	public void setLatitude(double l) { this.latitude = l; }

	public double getLongitude() { return longitude; }
	public void setLongitude(double l) { this.longitude = l; }
}
