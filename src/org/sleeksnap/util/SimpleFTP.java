/* 
Copyright Paul James Mutton, 2001-2004, http://www.jibble.org/

This file is part of SimpleFTP.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

$Author: pjm2 $
$Id: SimpleFTP.java,v 1.2 2004/05/29 19:27:37 pjm2 Exp $

 */

package org.sleeksnap.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.StringTokenizer;

/**
 * SimpleFTP is a simple package that implements a Java FTP client. With
 * SimpleFTP, you can connect to an FTP server and upload multiple files.
 * <p>
 * Copyright Paul Mutton, <a
 * href="http://www.jibble.org/">http://www.jibble.org/</a>
 * 
 */
public class SimpleFTP {

	private static boolean DEBUG = false;

	private BufferedReader reader = null;

	private Socket socket = null;

	private BufferedWriter writer = null;

	/**
	 * Create an instance of SimpleFTP.
	 */
	public SimpleFTP() {

	}

	/**
	 * Enter ASCII mode for sending text files. This is usually the default
	 * mode. Make sure you use binary mode if you are sending images or other
	 * binary data, as ASCII mode is likely to corrupt them.
	 */
	public synchronized boolean ascii() throws IOException {
		sendLine("TYPE A");
		final String response = readLine();
		return (response.startsWith("200 "));
	}

	/**
	 * Enter binary mode for sending binary files.
	 */
	public synchronized boolean bin() throws IOException {
		sendLine("TYPE I");
		final String response = readLine();
		return (response.startsWith("200 "));
	}

	/**
	 * Connects to the default port of an FTP server and logs in as
	 * anonymous/anonymous.
	 */
	public synchronized void connect(final String host) throws IOException {
		connect(host, 21);
	}

	/**
	 * Connects to an FTP server and logs in as anonymous/anonymous.
	 */
	public synchronized void connect(final String host, final int port)
			throws IOException {
		connect(host, port, "anonymous", "anonymous");
	}

	/**
	 * Connects to an FTP server and logs in with the supplied username and
	 * password.
	 */
	public synchronized void connect(final String host, final int port,
			final String user, final String pass) throws IOException {
		if (socket != null) {
			throw new IOException(
					"SimpleFTP is already connected. Disconnect first.");
		}
		socket = new Socket(host, port);
		reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(
				socket.getOutputStream()));

		String response = readLine();
		if (!response.startsWith("220 ")) {
			throw new IOException(
					"SimpleFTP received an unknown response when connecting to the FTP server: "
							+ response);
		}

		sendLine("USER " + user);

		response = readLine();
		if (!response.startsWith("331 ")) {
			throw new IOException(
					"SimpleFTP received an unknown response after sending the user: "
							+ response);
		}

		sendLine("PASS " + pass);

		response = readLine();
		if (!response.startsWith("230 ")) {
			throw new IOException(
					"SimpleFTP was unable to log in with the supplied password: "
							+ response);
		}

		// Now logged in.
	}

	/**
	 * Changes the working directory (like cd). Returns true if successful.
	 */
	public synchronized boolean cwd(final String dir) throws IOException {
		sendLine("CWD " + dir);
		final String response = readLine();
		return (response.startsWith("250 "));
	}

	/**
	 * Disconnects from the FTP server.
	 */
	public synchronized void disconnect() throws IOException {
		try {
			sendLine("QUIT");
		} finally {
			socket = null;
		}
	}

	/**
	 * Returns the working directory of the FTP server it is connected to.
	 */
	public synchronized String pwd() throws IOException {
		sendLine("PWD");
		String dir = null;
		final String response = readLine();
		if (response.startsWith("257 ")) {
			final int firstQuote = response.indexOf('\"');
			final int secondQuote = response.indexOf('\"', firstQuote + 1);
			if (secondQuote > 0) {
				dir = response.substring(firstQuote + 1, secondQuote);
			}
		}
		return dir;
	}

	private String readLine() throws IOException {
		final String line = reader.readLine();
		if (DEBUG) {
			System.out.println("< " + line);
		}
		return line;
	}

	/**
	 * Sends a raw command to the FTP server.
	 */
	private void sendLine(final String line) throws IOException {
		if (socket == null) {
			throw new IOException("SimpleFTP is not connected.");
		}
		try {
			writer.write(line + "\r\n");
			writer.flush();
			if (DEBUG) {
				System.out.println("> " + line);
			}
		} catch (final IOException e) {
			socket = null;
			throw e;
		}
	}

	/**
	 * Sends a file to be stored on the FTP server. Returns true if the file
	 * transfer was successful. The file is sent in passive mode to avoid NAT or
	 * firewall problems at the client end.
	 */
	public synchronized boolean stor(final File file) throws IOException {
		if (file.isDirectory()) {
			throw new IOException("SimpleFTP cannot upload a directory.");
		}

		final String filename = file.getName();

		return stor(new FileInputStream(file), filename);
	}

	/**
	 * Sends a file to be stored on the FTP server. Returns true if the file
	 * transfer was successful. The file is sent in passive mode to avoid NAT or
	 * firewall problems at the client end.
	 */
	public synchronized boolean stor(final InputStream inputStream,
			final String filename) throws IOException {

		final BufferedInputStream input = new BufferedInputStream(inputStream);

		sendLine("PASV");
		String response = readLine();
		if (!response.startsWith("227 ")) {
			throw new IOException("SimpleFTP could not request passive mode: "
					+ response);
		}

		String ip = null;
		int port = -1;
		final int opening = response.indexOf('(');
		final int closing = response.indexOf(')', opening + 1);
		if (closing > 0) {
			final String dataLink = response.substring(opening + 1, closing);
			final StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");
			try {
				ip = tokenizer.nextToken() + "." + tokenizer.nextToken() + "."
						+ tokenizer.nextToken() + "." + tokenizer.nextToken();
				port = Integer.parseInt(tokenizer.nextToken()) * 256
						+ Integer.parseInt(tokenizer.nextToken());
			} catch (final Exception e) {
				throw new IOException(
						"SimpleFTP received bad data link information: "
								+ response);
			}
		}

		sendLine("STOR " + filename);

		final Socket dataSocket = new Socket(ip, port);

		response = readLine();
		if (!response.startsWith("150 ")) {
			throw new IOException(
					"SimpleFTP was not allowed to send the file: " + response);
		}

		final BufferedOutputStream output = new BufferedOutputStream(
				dataSocket.getOutputStream());
		final byte[] buffer = new byte[4096];
		int bytesRead = 0;
		while ((bytesRead = input.read(buffer)) != -1) {
			output.write(buffer, 0, bytesRead);
		}
		output.flush();
		output.close();
		input.close();

		response = readLine();
		return response.startsWith("226 ");
	}

}