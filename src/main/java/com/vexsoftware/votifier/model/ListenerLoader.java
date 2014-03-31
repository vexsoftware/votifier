package com.vexsoftware.votifier.model;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads vote listeners. Listeners that cannot be instantiated will be skipped.
 */
public class ListenerLoader {

	/** The logger instance. */
	private static final Logger LOG = Logger.getLogger("Votifier");

	/**
	 * Loads all listener class files from a directory.
	 * 
	 * @param directory
	 *            The directory
	 */
	@SuppressWarnings("resource")
	public static List<VoteListener> load(String directory) {
		List<VoteListener> listeners = new ArrayList<VoteListener>();
		File dir = new File(directory);

		// Verify configured vote listener directory exists
		if (!dir.exists()) {
			LOG.warning("No listeners loaded! Cannot find listener directory '" + dir + "' ");
			return listeners;
		}

		// Load the vote listener instances.
		ClassLoader loader;
		try {
			loader = new URLClassLoader(new URL[] { dir.toURI().toURL() }, VoteListener.class.getClassLoader());
		} catch (MalformedURLException exception) {
			LOG.log(Level.SEVERE,
					"Error while configuring listener class loader",
					exception);
			return listeners;
		}
		
		for (File file : dir.listFiles()) {
			if (!file.getName().endsWith(".class")) {
				continue; // Only load class files!
			}
			String name = file.getName().substring(0, file.getName().lastIndexOf("."));
			try {
				Class<?> clazz = loader.loadClass(name);
				Object object = clazz.newInstance();
				if (!(object instanceof VoteListener)) {
					LOG.info("Not a vote listener: " + clazz.getSimpleName());
					continue;
				}
				VoteListener listener = (VoteListener) object;
				listeners.add(listener);
				LOG.info("Loaded vote listener: " + listener.getClass().getSimpleName());
			}
			/*
			 * Catch the usual definition and dependency problems with a loader
			 * and skip the problem listener.
			 */
			catch (Exception exception) {
				LOG.warning("Error loading '" + name + "' listener! Listener disabled.");
			} catch (Error exception) {
				LOG.warning("Error loading '" + name + "' listener! Listener disabled.");
			}
		}
		return listeners;
	}
}
