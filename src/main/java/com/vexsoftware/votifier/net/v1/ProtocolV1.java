package com.vexsoftware.votifier.net.v1;

import java.io.InputStream;
import java.io.OutputStream;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.net.Protocol;
import com.vexsoftware.votifier.util.rsa.RSA;

/**
 * Version 1 of the Votifier protocol.
 */
public class ProtocolV1 implements Protocol {

	/**
	 * Singleton.
	 */
	public static final ProtocolV1 INSTANCE = new ProtocolV1();

	private ProtocolV1() {
		// private
	}
	
	/**
	 * Handle the protocol
	 *
	 * @param plugin
	 *           Votifier plugin
	 * @param in
	 *           The receiving connection's input stream
	 * @param out
	 *           The receiving connection's output stream
	 * @param challenge
	 *           The challenge issued in this connection
	 * @throws Exception
	 *           If an error occurs
	 */
	public Vote handleProtocol(Votifier plugin, InputStream in, OutputStream out, String challenge) throws Exception {
		// Read the 256 byte block.
		byte[] block = new byte[256];
		in.read(block, 0, block.length);

		// Decrypt the block.
		block = RSA.decrypt(block, plugin.getKeyPair().getPrivate());
		int position = 0;

		// Perform the opcode check.
		String opcode = readString(block, position);
		position += opcode.length() + 1;
		if (!opcode.equals("VOTE")) {
			// Something went wrong in RSA.
			throw new Exception("Unable to decode RSA");
		}

		// Parse the block.
		String serviceName = readString(block, position);
		position += serviceName.length() + 1;
		String username = readString(block, position);
		position += username.length() + 1;
		String address = readString(block, position);
		position += address.length() + 1;
		String timeStamp = readString(block, position);
		position += timeStamp.length() + 1;
		
		return new Vote(serviceName, username, address, timeStamp);
	}
	
	/**
	 * Reads a string from a block of data.
	 * 
	 * @param data
	 *            The data to read from
	 * @return The string
	 */
	private static String readString(byte[] data, int offset) {
		StringBuilder builder = new StringBuilder();
		for (int i = offset; i < data.length; i++) {
			if (data[i] == '\n') {
				break;
			}
			builder.append((char) data[i]);
		}
		return builder.toString();
	}
	
}
