/*
 * Copyright (C) 2012 Vex Software LLC
 * This file is part of Votifier.
 * 
 * Votifier is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Votifier is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Votifier.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.vexsoftware.votifier.net;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.*;

import com.vexsoftware.votifier.Votifier;

/**
 * The vote receiving acceptor.
 */
public class VoteAcceptor extends Thread {

	/** The logger instance. */
	private static final Logger LOG = Logger.getLogger("Votifier");

	/** The plugin. */
	private final Votifier plugin;

	/** The host to listen on. */
	private final String host;

	/** The port to listen on. */
	private final int port;

	/** The server socket. */
	private ServerSocket server;

	/** The running flag. */
	private boolean running = true;

	/**
	 * Instantiates a new vote receiver.
	 * 
	 * @param host
	 *            The host to listen on
	 * @param port
	 *            The port to listen on
	 */
	public VoteAcceptor(final Votifier plugin, String host, int port) throws Exception {
		this.plugin = plugin;
		this.host = host;
		this.port = port;

		initialize();
	}

	private void initialize() throws Exception {
		try {
			server = new ServerSocket();
			server.bind(new InetSocketAddress(host, port));
		} catch (Exception exception) {
			LOG.severe("Error initializing vote receiver. Please verify that the configured");
			LOG.severe("IP address and port are not already in use. This is a common problem");
			LOG.log(Level.SEVERE,
					"with hosting services and, if so, you should check with your hosting provider.",
					exception);
			throw exception;
		}
	}

	/**
	 * Shuts the vote receiver down cleanly.
	 */
	public void shutdown() {
		running = false;
		if (server == null) {
			return;
		}
		try {
			server.close();
		} catch (Exception exception) {
			LOG.warning("Unable to shut down vote receiver cleanly.");
		}
	}

	public void run() {
		while (running) {
			try {
				Socket socket = server.accept();
				socket.setSoTimeout(5000); // Don't hang on slow connections.
				new Thread(new VoteClientThread(plugin, socket)).start();
			} catch (SocketException exception) {
				LOG.warning("Protocol error. Ignoring packet - " + exception.getLocalizedMessage());
			} catch (Exception exception) {
				LOG.log(Level.WARNING,
						"Exception caught while receiving a vote notification",
						exception);
			}
		}
	}
	
}
