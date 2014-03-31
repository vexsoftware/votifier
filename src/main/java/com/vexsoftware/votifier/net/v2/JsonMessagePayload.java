package com.vexsoftware.votifier.net.v2;

/**
 * Json message payload
 */
public class JsonMessagePayload {

	/**
	 * Vote domain.
	 */
	public String domain;
	
	/**
	 * Vote username.
	 */
	public String username;
	
	/**
	 * Vote address.
	 */
	public String address;
	
	/**
	 * Vote unix timestamp.
	 */
	public long timestamp;
	
	/**
	 * 
	 * @return Vote domain.
	 */
	public String getDomain() {
		return domain;
	}

	/**
	 * 
	 * @return Vote username.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * 
	 * @return Vote address.
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * 
	 * @return Vote unix timestamp.
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
}
