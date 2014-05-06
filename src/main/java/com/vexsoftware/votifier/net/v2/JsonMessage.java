package com.vexsoftware.votifier.net.v2;

/**
 * Json message
 */
public class JsonMessage {

	/**
	 * The service.
	 */
	public String service;
	
	/**
	 * The signature of the payload.
	 */
	public String signature;
	
	/**
	 * Json representation of a VoteMessagePayload
	 */
	public String payload;

	/**
	 * 
	 * @return The service.
	 */
	public String getService() {
		return service;
	}

	/**
	 * 
	 * @return The signature of the payload.
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * 
	 * @return Json representation of a VoteMessagePayload
	 */
	public String getPayload() {
		return payload;
	}
	
}
