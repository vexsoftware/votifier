/*
 * Copyright (C) 2011 Vex Software LLC
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

package com.vexsoftware.votifier.util.rsa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.xml.bind.DatatypeConverter;

import net.minecraft.util.org.apache.commons.io.IOUtils;

/**
 * Static utility methods for saving and loading RSA key pairs.
 */
public class RSAIO {

	/**
	 * Saves the key pair to the disk.
	 * 
	 * @param directory
	 *            The directory to save to
	 * @param keyPair
	 *            The key pair to save
	 * @throws Exception
	 *            If an error occurs
	 */
	public static void save(File directory, KeyPair keyPair) throws Exception {
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();

		// Store the public key.
		X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publicKey.getEncoded());
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(directory + "/public.key");
			out.write(DatatypeConverter.printBase64Binary(publicSpec.getEncoded()).getBytes());
		} finally {
			try {
				out.close();
			} catch(Exception exception) {
				// ignore
			}
		}

		// Store the private key.
		PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
		try {
			out = new FileOutputStream(directory + "/private.key");
			out.write(DatatypeConverter.printBase64Binary(privateSpec.getEncoded()).getBytes());
		} finally {
			try {
				out.close();
			} catch(Exception exception) {
				// ignore
			}
		}
	}

	/**
	 * Loads an RSA key pair from a directory. The directory must have the files
	 * "public.key" and "private.key".
	 * 
	 * @param directory
	 *            The directory to load from
	 * @return The key pair
	 * @throws Exception
	 *             If an error occurs
	 */
	public static KeyPair load(File directory) throws Exception {
		// Read the public key file.
		File publicKeyFile = new File(directory + "/public.key");
		FileInputStream in = null;
		byte[] encodedPublicKey;
		try {
			in = new FileInputStream(directory + "/public.key");
			encodedPublicKey = new byte[(int) publicKeyFile.length()];
			in.read(encodedPublicKey);
			encodedPublicKey = DatatypeConverter.parseBase64Binary(new String(encodedPublicKey));
		} finally {
			try {
				in.close();
			} catch(Exception exception) {
				// ignore
			}
		}

		// Read the private key file.
		File privateKeyFile = new File(directory + "/private.key");
		byte[] encodedPrivateKey;
		try {
			in = new FileInputStream(directory + "/private.key");
			encodedPrivateKey = new byte[(int) privateKeyFile.length()];
			in.read(encodedPrivateKey);
			encodedPrivateKey = DatatypeConverter.parseBase64Binary(new String(encodedPrivateKey));
		} finally {
			try {
				in.close();
			} catch(Exception exception) {
				// ignore
			}
		}

		// Instantiate and return the key pair.
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
		return new KeyPair(publicKey, privateKey);
	}
	
	/**
	 * Loads an RSA public key from a URL.
	 * 
	 * @param url
	 *            The URL that has the public key
	 * @return
	 *            The public key
	 * @throws Exception
	 *            If an error occurs
	 */
	public static PublicKey loadPublicKey(URL url) throws Exception {
		String publicKey = new String(IOUtils.toByteArray(url), "UTF-8").replaceAll("(-+BEGIN PUBLIC KEY-+\\r?\\n|-+END PUBLIC KEY-+\\r?\\n?)", "");
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(DatatypeConverter.parseBase64Binary(publicKey));
		return keyFactory.generatePublic(publicKeySpec);
	}

}
