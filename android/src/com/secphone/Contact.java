package com.secphone;

public class Contact {
	String name;
	String email;
	String publicKey;
	
	public Contact(String name, String email,String publicKey) {
		this.name = name;
		this.email = email;
		this.publicKey = publicKey;
	}
	
	public String getName() { return name; }
	public String getEmail() { return email; }
	public String getPublicKey() { return publicKey; }
}
