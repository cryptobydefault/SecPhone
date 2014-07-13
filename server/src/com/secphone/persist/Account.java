package com.secphone.persist;

import java.util.*;
import java.util.regex.*;
import com.secphone.util.Password;

public class Account {
	long			id;
	String			email;
	String			name = null;
	int				status;
	String			validationCode;
	String			publicKeys;
	// byte[] 		passwordSalt;
	// byte[]		passwordHash;
	
	public static final int STATUS_UNVERIFIED = 0;
	public static final int STATUS_NO_KEYS = 1;
	public static final int STATUS_VALID_KEYS = 2;

	public Account() { }

	public Account(String e) {
		this.email = e;
		this.status = STATUS_UNVERIFIED;
		this.name = null;
		this.validationCode = null;
		this.publicKeys = null;

		// Password password = new Password(p);
		// this.passwordSalt = password.getSalt();
		// this.passwordHash = password.getHash();
	}

	public void persist() 
	throws PersistException {
		(new Persist()).persist(this);
	}

	public void update() 
	throws PersistException {
		(new Persist()).update(this);
	}

	public static Account queryByEmail(String e) {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("email", e);
		
		return (Account) (new Persist()).doNamedQuery("accountByEmail", 
							 params, false, 0);			
	}

	public long getId() { return id; }
	public void setId(long id) { this.id = id; }

	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getName() { return name; }
	public void setName(String n) { this.name = n; }
	
	public int getStatus() { return status; }
	public void setStatus(int s) { this.status = s; }
	
	public String getValidationCode() { return validationCode; }
	public void setValidationCode(String v) { this.validationCode = v; }
	
	public String getPublicKeys() { return publicKeys; }
	public void setPublicKeys(String k) { this.publicKeys = k; }
/*
	public byte[] getPasswordSalt() { return passwordSalt; }
	public void setPasswordSalt(byte[] salt) { this.passwordSalt = salt; }

	public byte[] getPasswordHash() { return passwordHash; }
	public void setPasswordHash(byte[] ph) { this.passwordHash = ph; }
*/
}
