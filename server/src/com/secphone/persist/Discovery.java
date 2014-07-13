package com.secphone.persist;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.Expose;

public class Discovery {
	long 					id;
	@Expose String		mac;
	@Expose Date		timestamp;
	@Expose double		longitude;
	@Expose double		latitude;
	@Expose double		accuracy;

	public Discovery() {
	}
	
	public Discovery(String mac, Date t, double lat, double lon, double a) {
		this.mac = mac;
		this.timestamp = t;
		this.latitude = lat;
		this.longitude = lon;
		this.accuracy = a;
	}
	
	public void persist() 
	throws PersistException {
		(new Persist()).persist(this);
	}

	public void update() 
	throws PersistException {
		(new Persist()).update(this);
	}

	public static Discovery queryLatestByMac(String m) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("mac", m);
		
		return (Discovery) (new Persist()).doNamedQuery("latestByMac", 
							 params, false, 1);			
	}

	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getMac() {
		return mac;
	}
	public void setMac(String mac) {
		this.mac = mac;
	}
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getAccuracy() {
		return accuracy;
	}
	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}
}
