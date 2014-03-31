package com.vexsoftware.votifier.net.v2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.bukkit.craftbukkit.libs.com.google.gson.Gson;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.Protocol;
import com.vexsoftware.votifier.util.rsa.RSA;

/**
 * Version 2 of the Votifier protocol.
 */
public class ProtocolV2 implements Protocol {

	/**
	 * Singleton.
	 */
	public static final ProtocolV2 INSTANCE = new ProtocolV2();
	
	/**
	 * Our instance of Gson.
	 */
	public static final Gson GSON = new Gson();
	
	private Set<String> replayCache = new HashSet<String>();

	private ProtocolV2() {
		// private
	}
	
	/**
	 * Handle the protocol
	 *
	 * @param plugin
	 *           Votifier plugin
	 * @param socket
	 *           The receiving connection
	 * @throws Exception
	 *           If an error occurs
	 */
	public Vote handleProtocol(Votifier plugin, InputStream in, OutputStream out) throws Exception {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(in));
			JsonMessage jsonMessage = GSON.fromJson(reader.readLine(), JsonMessage.class);
			
			String service = jsonMessage.getService();
			PublicKey publicKey = plugin.getWebsites().get(service);
			// Check if the service exists
			if (publicKey == null) {
				throw new Exception("Unknown service: " + service);
			}
			String payload = jsonMessage.getPayload();
			// Check the RSA signature
			if (!RSA.verify(payload.getBytes("UTF-8"), DatatypeConverter.parseBase64Binary(jsonMessage.getSignature().trim()), publicKey)) {
				throw new Exception("Signature not valid");
			}
			
			JsonMessagePayload jsonMessagePayload = GSON.fromJson(jsonMessage.getPayload(), JsonMessagePayload.class);
			// Check for a replay attack
			if (replayCache.contains(jsonMessage.getSignature().trim() + "\0" + jsonMessagePayload.getTimestamp())) {
				throw new Exception("Replay attempt");
			}
			// Check if the vote is older than 10 minutes. If so, it has expired and can no longer be redeemed
			if (System.currentTimeMillis() - jsonMessagePayload.getTimestamp() > 10L * 60L * 1000L) { // 10 minutes
				throw new Exception("Vote expired. Is your system time correct?");
			}
			
			return new Vote(service, jsonMessagePayload.getUsername(), jsonMessagePayload.getAddress(), jsonMessagePayload.getTimestamp());
		} finally {
			if(reader != null) {
				try {
					reader.close();
				} catch(Exception exception) {
					// ignore
				}
			}
		}
	}
	
}
