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

package com.vexsoftware.votifier;

import java.io.File;
import java.net.URL;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.vexsoftware.votifier.model.ListenerLoader;
import com.vexsoftware.votifier.model.VoteListener;
import com.vexsoftware.votifier.net.VoteAcceptor;
import com.vexsoftware.votifier.util.LogFilter;
import com.vexsoftware.votifier.util.rsa.RSAIO;
import com.vexsoftware.votifier.util.rsa.RSAKeygen;

/**
 * The main Votifier plugin class.
 */
public class Votifier extends JavaPlugin {

	/** The logger instance. */
	private static final Logger LOG = Logger.getLogger("Votifier");

	/** Log entry prefix */
	private static final String logPrefix = "[Votifier] ";

	/** The current Votifier version. */
	private String version;

	/** The vote listeners. */
	private List<VoteListener> listeners = new ArrayList<VoteListener>();
	
	/** The websites mapped to their public key. */
	private Map<String, PublicKey> websites = new HashMap<String, PublicKey>();

	/** The vote receiver. */
	private VoteAcceptor voteAcceptor;

	/** The RSA key pair. */
	private KeyPair keyPair;

	/** Debug mode flag */
	private boolean debug;
	
	/** Old protocol flag */
	private boolean oldProtocol;

	/**
	 * Attach custom log filter to logger.
	 */
	static {
		LOG.setFilter(new LogFilter(logPrefix));
	}
	
	@Override
	public void onLoad() {
		FileConfiguration config = super.getConfig();
		
		if (!new File(getDataFolder() + "/config.yml").exists()) {
			LOG.info("Configuring Votifier for the first time...");
			LOG.info("------------------------------------------------------------------------------");
			LOG.info("Assigning Votifier to listen on port 8192. If you are hosting Craftbukkit on a");
			LOG.info("shared server please check with your hosting provider to verify that this port");
			LOG.info("is available for your use. Chances are that your hosting provider will assign");
			LOG.info("a different port, which you need to specify in config.yml");
			LOG.info("------------------------------------------------------------------------------");
			
			/*
			 * Use IP address from server.properties as a default for
			 * configurations. Do not use InetAddress.getLocalHost() as it most
			 * likely will return the main server address instead of the address
			 * assigned to the server.
			 */
			String hostAddr = Bukkit.getServer().getIp();
			if (hostAddr == null || hostAddr.length() == 0) {
				hostAddr = "0.0.0.0";
			}
			config.set("host", hostAddr);
			
			// Replace to remove a bug with Windows paths - SmilingDevil
			config.set("listener_folder", getDataFolder().toString().replace("\\", "/") + "/listeners");
		}
		
		if (config.contains("websites")) {
			return;
		}
		
		config.options().copyDefaults(true);
		saveConfig();
		reloadConfig();
	}

	@Override
	public void onEnable() {
		// Set the plugin version.
		version = getDescription().getVersion();

		// Handle configuration.
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}
		FileConfiguration config = super.getConfig();
		File rsaDirectory = new File(getDataFolder() + "/rsa");
		File listenerDirectory = new File(config.getString("listener_folder"));
		listenerDirectory.mkdir();

		/*
		 * Create RSA directory and keys if it does not exist; otherwise, read
		 * keys.
		 */
		try {
			if (!rsaDirectory.exists()) {
				rsaDirectory.mkdir();
				keyPair = RSAKeygen.generate(2048);
				RSAIO.save(rsaDirectory, keyPair);
			} else {
				keyPair = RSAIO.load(rsaDirectory);
			}
		} catch (Exception exception) {
			LOG.log(Level.SEVERE,
					"Error reading configuration file or RSA keys", 
					exception);
			gracefulExit();
			return;
		}

		// Load the vote listeners.
		listeners.addAll(ListenerLoader.load(listenerDirectory));
		
		// Load the website public keys
		for (Entry<String, Object> website : config.getConfigurationSection("websites").getValues(false).entrySet()) {
			try {
				websites.put(website.getKey(), RSAIO.loadPublicKey(new URL((String) website.getValue())));
				if (!website.getKey().startsWith("https://")) {
					LOG.warning("You are loading a public key (" + website.getKey() + ") over a non-SSL connection. This is insecure!");
				}
				LOG.info("Loaded public key for website: " + website.getKey());
			} catch (Exception exception) {
				LOG.log(Level.WARNING,
						"Error loading public key for website: " + website.getKey(), 
						exception);
			}
		}

		// Initialize the acceptor.
		String host = config.getString("host");
		int port = config.getInt("port");
		oldProtocol = config.getBoolean("enable_old_protocol");
		debug = config.getBoolean("debug");
		if (debug) {
			LOG.info("DEBUG mode enabled!");
		}

		try {
			voteAcceptor = new VoteAcceptor(this, host, port);
			voteAcceptor.start();

			LOG.info("Votifier enabled.");
		} catch (Exception exception) {
			gracefulExit();
			return;
		}
	}

	@Override
	public void onDisable() {
		// Interrupt the vote acceptor.
		if (voteAcceptor != null) {
			voteAcceptor.shutdown();
		}
		LOG.info("Votifier disabled.");
	}

	private void gracefulExit() {
		LOG.log(Level.SEVERE, "Votifier did not initialize properly!");
	}

	/**
	 * Gets the version.
	 * 
	 * @return The version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Gets the listeners.
	 * 
	 * @return The listeners
	 */
	public List<VoteListener> getListeners() {
		return listeners;
	}
	
	/**
	 * Gets the websites mapped to their public key
	 * 
	 * @return The websites
	 */
	public Map<String, PublicKey> getWebsites() {
		return websites;
	}

	/**
	 * Gets the vote acceptor.
	 * 
	 * @return The vote acceptor
	 */
	public VoteAcceptor getVoteAcceptor() {
		return voteAcceptor;
	}

	/**
	 * Gets the keyPair.
	 * 
	 * @return The keyPair
	 */
	public KeyPair getKeyPair() {
		return keyPair;
	}

	public boolean isDebug() {
		return debug;
	}
	
	public boolean isOldProtocol() {
		return oldProtocol;
	}

}
