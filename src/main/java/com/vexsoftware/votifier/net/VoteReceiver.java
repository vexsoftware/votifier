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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

import com.vexsoftware.votifier.Votifier;

/**
 * The vote receiving server.
 * 
 * @author Blake Beaupain
 * @author Kramer Campbell
 */
public class VoteReceiver extends Thread {

	/** The logger instance. */
	private static final Logger LOG = Logger.getLogger("Votifier");

	private final Votifier plugin;

	/** The host to listen on. */
	private final String host;

	/** The port to listen on. */
	private final int port;

	/** The server socket. */
	private ServerSocket server;

	/** The running flag. */
	private boolean running = true;

	public Map<InetAddress, Integer> badPacketCounter = new HashMap<>();

	public void countBadPacket(InetAddress i){
		Integer a = badPacketCounter.get(i);
		if(a == null) a = 1;
		else a++;
		badPacketCounter.put(i,a);
	}

	/**
	 * Instantiates a new vote receiver.
	 * 
	 * @param host
	 *            The host to listen on
	 * @param port
	 *            The port to listen on
	 */
	public VoteReceiver(final Votifier plugin, String host, int port)
			throws Exception {
		this.plugin = plugin;
		this.host = host;
		this.port = port;

		initialize();
	}

	private void initialize() throws Exception {
		try {
			server = new ServerSocket();
			server.bind(new InetSocketAddress(host, port));
		} catch (Exception ex) {
			LOG.log(Level.SEVERE,
					"Error initializing vote receiver. Please verify that the configured");
			LOG.log(Level.SEVERE,
					"IP address and port are not already in use. This is a common problem");
			LOG.log(Level.SEVERE,
					"with hosting services and, if so, you should check with your hosting provider.",
					ex);
			throw new Exception(ex);
		}
	}

	/**
	 * Shuts the vote receiver down cleanly.
	 */
	public void shutdown() {
		running = false;
		if (server == null)
			return;
		try {
			server.close();
		} catch (Exception ex) {
			LOG.log(Level.WARNING, "Unable to shut down vote receiver cleanly.");
		}
	}

	@Override
	public void run() {

		// Main loop.
		while (running) {
			try {
				Socket socket = server.accept();

				if(socket == null) continue;

				if(Votifier.getInstance().getBadPacketThreshold() != -1){
					Integer badPackets = badPacketCounter.get(getRemoteAddress(socket));
					if(badPackets != null && badPackets >= Votifier.getInstance().getBadPacketThreshold()){
						LOG.severe("Host that exceeded the bad host threshold and has been blocked: "+socket.getInetAddress().getHostAddress());
						OutputStreamWriter writer = null;
						try {
							writer = new OutputStreamWriter(socket.getOutputStream());
							VoteHandler.bad(writer);
						} catch(IOException e){
							LOG.log(Level.WARNING,"Failed to send bad signal through vote socket.",e);
						}
						finally{
							if(writer != null){
								writer.close();
							}
						}
						socket.close();
						continue;
					}
				}

				new VoteHandler(socket, this).start();
			} catch (Exception e) {
				if(!running && e instanceof SocketException){
					return;
				}
				LOG.log(Level.WARNING, "Various exception occured while processing socket - " + e.getLocalizedMessage(), e);
			}
		}
	}

	public static InetAddress getRemoteAddress(Socket s){
		return ((InetSocketAddress) s.getRemoteSocketAddress()).getAddress();
	}
}
