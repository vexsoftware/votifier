package com.vexsoftware.votifier.net;

import java.io.InputStream;
import java.io.OutputStream;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.model.Vote;

/**
 * An interface to implement the Votifier protocol.
 */
public interface Protocol {

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
	public Vote handleProtocol(Votifier plugin, InputStream in, OutputStream out, String challenge) throws Exception;
	
}
