package com.vexsoftware.votifier.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;

import com.vexsoftware.votifier.Votifier;
import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VoteListener;
import com.vexsoftware.votifier.model.VotifierEvent;
import com.vexsoftware.votifier.net.v1.ProtocolV1;
import com.vexsoftware.votifier.net.v2.ProtocolV2;

/**
 * The vote client thread.
 */
public class VoteClientThread implements Runnable {

	/** The logger instance. */
	private static final Logger LOG = Logger.getLogger("Votifier");
	
	/** The plugin. */
	private Votifier plugin;
	
	/** The socket. */
	private Socket socket;

	public VoteClientThread(Votifier plugin, Socket socket) {
		this.plugin = plugin;
		this.socket = socket;
	}

	public void run() {
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			inputStream = new BufferedInputStream(socket.getInputStream());
			outputStream = new BufferedOutputStream(socket.getOutputStream());

			// Send them our version.
			outputStream.write(("VOTIFIER " + plugin.getVersion() + "\r\n").getBytes("UTF-8"));
			outputStream.flush();
			
			// Which protocol do we use?
			inputStream.mark(1);
			int firstByte = inputStream.read();
			inputStream.reset();
			
			Protocol protocol;
			if (firstByte == (int) '{') {
				protocol = ProtocolV2.INSTANCE;
			} else if (this.plugin.isOldProtocol()) {
				protocol = ProtocolV1.INSTANCE;
			} else {
				throw new Exception("Unknown protocol");
			}

			// Read the vote
			final Vote vote = protocol.handleProtocol(plugin, inputStream, outputStream);
			
			if (plugin.isDebug()) {
				LOG.info("Received vote record -> " + vote);
			}
			
			// Dispatch the vote to all listeners.
			for (VoteListener listener : plugin.getListeners()) {
				try {
					listener.voteMade(vote);
				} catch (Exception exception) {
					String vlName = listener.getClass().getSimpleName();
					LOG.log(Level.WARNING,
							"Exception caught while sending the vote notification to the '" + vlName + "' listener", 
							exception);
				}
			}

			// Call event in a synchronized fashion to ensure that the
			// custom event runs in the
			// the main server thread, not this one.
			plugin.getServer().getScheduler().runTask(plugin, new Runnable() {
				public void run() {
					plugin.getServer().getPluginManager().callEvent(new VotifierEvent(vote));
				}
			});
		} catch (BadPaddingException exception) {
			LOG.warning("Unable to decrypt vote record. Make sure that that your public key");
			LOG.log(Level.WARNING,
					"matches the one you gave the server list.",
					exception);
		} catch(Exception exception) {
			LOG.log(Level.WARNING,
					"Exception caught while receiving a vote notification",
					exception);
		} finally {
			if (outputStream != null) {
				try {
					inputStream.close();
				} catch(Exception exception) {
					// ignore
				}
			}
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch(Exception exception) {
					// ignore
				}
			}
			if (socket != null) {
				try {
					socket.close();
				} catch(Exception exception) {
					// ignore
				}
			}
		}
	}

}
