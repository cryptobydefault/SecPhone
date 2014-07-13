package com.secphone.persist;

import java.util.*;
import java.util.regex.*;
import com.secphone.util.Password;

public class App {
	long				id;
	String			email;
	String			name;
	byte[] 			passwordSalt;
	byte[]			passwordHash;
	Set<Device>		devices = new HashSet<Device>();
	Set<Attribute>	attributes = new HashSet<Attribute>();

	public App() { }

	String generateId() {
		return null;
	}

	public App(String e, String n, String p) {
		this.email = e;
		this.name = n; 

		Password password = new Password(p);
		this.passwordSalt = password.getSalt();
		this.passwordHash = password.getHash();
	}

	public void addDevice(Device d) {
		d.setApp(this);
		devices.add(d);
	}

	public void addAttribute(Attribute a) {
		a.setApp(this);
		attributes.add(a);
	}

	public void persist() 
	throws PersistException {
		(new Persist()).persist(this);
	}

	public void update() 
	throws PersistException {
		(new Persist()).update(this);
	}

	public static App queryByEmail(String e) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("email", e);
		
		return (App) (new Persist()).doNamedQuery("appByEmail", 
							 params, false, 0);			
	}

	public long getId() { return id; }
	public void setId(long id) { this.id = id; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getName() { return name; }
	public void setName(String n) { this.name = n; }

	public byte[] getPasswordSalt() { return passwordSalt; }
	public void setPasswordSalt(byte[] salt) { this.passwordSalt = salt; }

	public byte[] getPasswordHash() { return passwordHash; }
	public void setPasswordHash(byte[] ph) { this.passwordHash = ph; }

	public Set<Device> getDevices() { return devices; }
	public void setDevices(Set<Device> devices) { this.devices = devices; }

	public Set<Attribute> getAttributes() { return attributes; }
	public void setAttributes(Set<Attribute> attributes) { this.attributes = attributes; }
}

