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
	 * @param socket
	 *           The receiving connection
	 * @throws Exception
	 *           If an error occurs
	 */
	public Vote handleProtocol(Votifier plugin, InputStream in, OutputStream out) throws Exception;
	
}
