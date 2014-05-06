package com.vexsoftware.votifier.net.v2;

/**
 * Json message payload
 */
public class JsonMessagePayload {

	/**
	 * Vote host.
	 */
	public String host;
	
	/**
	 * Vote username.
	 */
	public String username;
	
	/**
	 * Vote address.
	 */
	public String address;
	
	/**
	 * Vote challenge.
	 */
	private String challenge;
	
	/**
	 * Vote unix timestamp.
	 */
	public long timestamp;
	
	/**
	 * 
	 * @return Vote host.
	 */
	public String getHost() {
		return host;
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
	 * @return Vote challenge.
	 */
	public String getChallenge() {
		return challenge;
	}

	/**
	 * 
	 * @return Vote unix timestamp.
	 */
	public long getTimestamp() {
		return timestamp;
	}
	
}
