/**
 * Sleeksnap, the open source cross-platform screenshot uploader
 * Copyright (C) 2012 Nikki <nikki@nikkii.us>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sleeksnap.util;

import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.sleeksnap.Constants;
import org.sleeksnap.Constants.Application;
import org.sleeksnap.Constants.Version;

/**
 * A basic utility class
 * 
 * @author Nikki
 * 
 */
public class Util {
	/**
	 * An enum containing operating system types
	 * 
	 * @author Nikki
	 * 
	 */
	public static enum OperatingSystem {
		LINUX, MAC, SOLARIS, UNKNOWN, WINDOWS
	}

	/**
	 * A list of popular browsers
	 */
	private static final String[] BROWSERS = new String[] { "google-chrome",
			"firefox", "opera", "epiphany", "konqueror", "conkeror", "midori",
			"kazehakase", "mozilla" };

	/**
	 * The cached operating system
	 */
	public static final OperatingSystem SYSTEM = getPlatform();

	/**
	 * The saved working directory
	 */
	private static File workDir;

	public static void centerFrame(final JFrame frame) {
		final Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
		final int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
		final int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
		frame.setLocation(x, y);
	}

	/**
	 * A method to get the unix time...
	 * 
	 * @return The current time in seconds
	 */
	public static long currentTimeSeconds() {
		return (System.currentTimeMillis() / 1000);
	}

	/**
	 * Get the last part of a class name
	 * 
	 * @param key
	 *            The full name
	 * @return The class name formatted
	 */
	public static String formatClassName(final Class<?> key) {
		return key.getName().substring(key.getName().lastIndexOf('.') + 1);
	}

	/**
	 * Get the computer's user agent
	 * 
	 */
	public static String getHttpUserAgent() {
		return Application.NAME + " v" + Version.getVersionString();
	}

	/**
	 * Get the current platform
	 * 
	 * @return The current platform
	 */
	public static OperatingSystem getPlatform() {
		final String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("win")) {
			return OperatingSystem.WINDOWS;
		}
		if (osName.contains("mac")) {
			return OperatingSystem.MAC;
		}
		if (osName.contains("solaris")) {
			return OperatingSystem.SOLARIS;
		}
		if (osName.contains("sunos")) {
			return OperatingSystem.SOLARIS;
		}
		if (osName.contains("linux")) {
			return OperatingSystem.LINUX;
		}
		if (osName.contains("unix")) {
			return OperatingSystem.LINUX;
		}
		return OperatingSystem.UNKNOWN;
	}

	/**
	 * Get a resource, allows us to run it from source or jar
	 * 
	 * @param name
	 *            The resource name
	 * @return The URL of the resource, either in-jar or on the filesystem
	 */
	public static URL getResourceByName(final String name) {
		if (Utils.class.getResource(name) != null) {
			return Utils.class.getResource(name);
		} else {
			File file = null;
			if ((file = new File("resources" + name)).exists()
					|| (file = new File(Util.getWorkingDirectory(), "resources"
							+ name)).exists()) {
				try {
					return file.toURI().toURL();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	/**
	 * Get the system architecture
	 * 
	 * @return The system architecture integer
	 */
	public static int getSystemArch() {
		final String archs = System.getProperty("os.arch");
		return Integer.parseInt(archs.substring(archs.length() - 2));
	}

	/**
	 * Get the current working directory
	 * 
	 * @return The working directory for the application
	 */
	public static File getWorkingDirectory() {
		if (workDir == null) {
			workDir = getWorkingDirectory(Constants.Application.NAME
					.toLowerCase());
		}
		return workDir;
	}

	/**
	 * Get the AppData directory
	 * 
	 * @param applicationName
	 *            The application name
	 * @return The working directory
	 */
	public static File getWorkingDirectory(final String applicationName) {
		final String userHome = System.getProperty("user.home", ".");
		File workingDirectory;
		switch (getPlatform()) {
		case LINUX:
		case SOLARIS:
			final File config = new File(userHome, ".config");
			if (config.exists()) {
				workingDirectory = new File(config, applicationName + '/');
				workingDirectory.mkdirs();
			} else {
				workingDirectory = new File(userHome,
						'.' + applicationName + '/');
			}
			break;
		case WINDOWS:
			final String applicationData = System.getenv("APPDATA");
			if (applicationData != null) {
				workingDirectory = new File(applicationData, "."
						+ applicationName + '/');
			} else {
				workingDirectory = new File(userHome,
						'.' + applicationName + '/');
			}
			break;
		case MAC:
			workingDirectory = new File(userHome,
					"Library/Application Support/" + applicationName);
			break;
		default:
			workingDirectory = new File(userHome, applicationName + '/');
			break;
		}
		if ((!workingDirectory.exists()) && (!workingDirectory.mkdirs())) {
			throw new RuntimeException(
					"The working directory could not be created: "
							+ workingDirectory);
		}
		return workingDirectory;
	}

	/**
	 * Implode a list of values (Like php's implode)
	 * 
	 * @param list
	 *            The list to implode
	 * @param glue
	 *            The glue to use between values
	 * @return The imploded string
	 */
	public static String implodeList(final List<?> list, final String glue) {
		final StringBuilder builder = new StringBuilder();
		for (final Iterator<?> it = list.iterator(); it.hasNext();) {
			builder.append(it.next());
			if (it.hasNext()) {
				builder.append(glue);
			}
		}
		return builder.toString();
	}

	/**
	 * Open a URL using java.awt.Desktop or a couple different manual methods
	 * 
	 * @param url
	 *            The URL to open
	 * @throws Exception
	 *             If an error occurs attempting to open the url
	 */
	public static void openURL(final URL url) throws Exception {
		final Desktop desktop = Desktop.getDesktop();
		if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
			desktop.browse(url.toURI());
		} else {
			final OperatingSystem system = Util.getPlatform();
			switch (system) {
			case MAC:
				Class.forName("com.apple.eio.FileManager")
						.getDeclaredMethod("openURL",
								new Class[] { String.class })
						.invoke(null, new Object[] { url.toString() });
				break;
			case WINDOWS:
				Runtime.getRuntime()
						.exec(new String[] { "rundll32",
								"url.dll,FileProtocolHandler", url.toString() });
				break;
			default:
				String browser = null;
				for (final String b : BROWSERS) {
					final Process p = Runtime.getRuntime().exec(
							new String[] { "which", browser });
					if (p.waitFor() == 0) {
						browser = b;
						break;
					}
				}
				if (browser != null) {
					Runtime.getRuntime().exec(
							new String[] { browser, url.toString() });
				} else {
					throw new Exception("Unable to find browser");
				}
			}
		}
	}

	/**
	 * Parse commandline arguments
	 * 
	 * @param args
	 *            The arg array from the main method, or manual args
	 * @return The map containing the args
	 */
	public static HashMap<String, Object> parseArguments(final String[] args) {
		final HashMap<String, Object> arguments = new HashMap<String, Object>();
		for (String s : args) {
			if (s.startsWith("--")) {
				s = s.substring(2);
			} else if (s.startsWith("-")) {
				s = s.substring(1);
			}
			final int eqIdx = s.indexOf('=');
			final String key = s.substring(0, eqIdx != -1 ? eqIdx : s.length());
			Object value = true;
			if (eqIdx != -1) {
				value = s.substring(s.indexOf('=') + 1);
			}
			arguments.put(key, value);
		}
		return arguments;
	}

	/**
	 * A utility method used for parsing uploader data (like what is used in the
	 * Spinner type)
	 * 
	 * @param string
	 *            The string to parse
	 * @return The data map
	 */
	public static Map<String, String> parseDataList(final String string) {
		final Map<String, String> ret = new HashMap<String, String>();
		final String[] split = string.split(",");
		for (final String s : split) {
			final int idx = s.indexOf('=');
			if (idx != -1) {
				final String key = s.substring(0, idx);
				final String value = s.substring(idx + 1);

				ret.put(key, value);
			} else {
				ret.put(s, null);
			}
		}
		return ret;
	}

	/**
	 * A utility method for parsing response data (Useful for k: v etc)
	 * 
	 * @param input
	 *            The input string
	 * @param kvSeparator
	 *            The value separating key and value
	 * @param entrySeparator
	 *            The value separating entries
	 * @return The parsed map
	 */
	public static Map<String, String> parseKeyValues(final String input,
			final String kvSeparator, final String entrySeparator) {
		final Map<String, String> ret = new HashMap<String, String>();
		final String[] split = input.split(entrySeparator);
		for (final String s : split) {
			final int idx = s.indexOf(kvSeparator);
			if (idx != -1) {
				final String key = s.substring(0, idx);
				final String value = s.substring(idx + 1);

				ret.put(key, value);
			} else {
				ret.put(s, null);
			}
		}
		return ret;
	}

	/**
	 * Set the working directory, used when Sleeksnap is self-contained (Uses a
	 * directory which you can have on a flash drive)
	 * 
	 * @param workingDirectory
	 *            The directory
	 */
	public static void setWorkingDirectory(final File workingDirectory) {
		workDir = workingDirectory;
	}

	/**
	 * Trim the string's start/finish of the specified character
	 * 
	 * @param str
	 *            The string
	 * @param ch
	 *            The character
	 * @return The trimmed string
	 */
	public static String trim(String str, final char ch) {
		if ((str == null) || str.isEmpty()) {
			return str;
		} else if (str.length() == 1) {
			return str.charAt(0) == ch ? "" : str;
		}
		try {
			if (str.charAt(0) == ch) {
				str = str.substring(1);
			}
			final int l = str.length() - 1;
			if (str.charAt(l) == ch) {
				str = str.substring(0, l);
			}
			return str;
		} catch (final Exception e) {
			return str;
		}
	}

	/**
	 * Make each word in a string uppercase
	 * 
	 * @param string
	 *            The string to parse
	 * @return The formatted string
	 */
	public static String ucwords(final String string) {
		final StringBuilder out = new StringBuilder();
		final String[] split = string.split(" ");
		for (int i = 0; i < split.length; i++) {
			final String str = split[i];
			out.append(Character.toUpperCase(str.charAt(0)));
			if (str.length() > 1) {
				out.append(str.substring(1).toLowerCase());
			}
			if (i < (split.length - 1)) {
				out.append(" ");
			}
		}
		return out.toString();
	}
}
