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
package org.sleeksnap.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import org.json.JSONException;
import org.json.JSONObject;
import org.sleeksnap.Constants.Application;
import org.sleeksnap.Constants.Version;
import org.sleeksnap.Launcher;
import org.sleeksnap.http.HttpUtil;
import org.sleeksnap.updater.Downloader.DownloadAdapter;
import org.sleeksnap.util.DesktopEntryBuilder;
import org.sleeksnap.util.Util;
import org.sleeksnap.util.Utils.FileUtils;
import org.sleeksnap.util.WinRegistry;

import com.sun.jna.Platform;

/**
 * An automatic Updater for Sleeksnap
 * 
 * Well documented to explain how it works and that it is not malicious.
 * 
 * @author Nikki
 * 
 */
public class Updater {

	public enum VerificationMode {
		INSERT, REMOVE, VERIFY
	}

	private static final Logger logger = Logger.getLogger(Updater.class
			.getName());

	/**
	 * If on windows or linux, verify that the registry entry is intact and
	 * pointing to the correct version.
	 * 
	 * @param file
	 *            The file to verify against
	 * @param mode
	 *            The mode to use, REMOVE will remove the entry, VERIFY will
	 *            update it, and INSERT will insert if it does not exist.
	 */
	public static void verifyAutostart(final File file,
			final VerificationMode mode) {
		if (Platform.isWindows()) {
			try {
				final String current = WinRegistry.readString(
						WinRegistry.HKEY_CURRENT_USER, WinRegistry.RUN_PATH,
						Application.NAME);
				if (mode == VerificationMode.REMOVE) {
					WinRegistry.deleteValue(WinRegistry.HKEY_CURRENT_USER,
							WinRegistry.RUN_PATH, Application.NAME);
				} else if (mode == VerificationMode.INSERT && current == null
						|| current != null
						&& !current.equals(file.getAbsolutePath())) {
					WinRegistry.writeStringValue(WinRegistry.HKEY_CURRENT_USER,
							WinRegistry.RUN_PATH, Application.NAME,
							file.getAbsolutePath());
				}
			} catch (final Exception e) {
				// Ignore it.
			}
		} else if (Platform.isX11()) {
			final File autostartDir = new File(
					System.getenv("XDG_CONFIG_HOME") != null ? System
							.getenv("XDG_CONFIG_HOME") : System
							.getProperty("user.home") + "/.config/autostart");
			if (autostartDir.exists()) {
				final File autostart = new File(autostartDir,
						Application.NAME.toLowerCase() + ".desktop");

				if (mode == VerificationMode.REMOVE) {
					autostart.delete();
				} else {
					final String contents = new DesktopEntryBuilder()
							.addEntry("Type", "Application")
							.addEntry("Categories", "Accessories")
							.addEntry("Name", Application.NAME)
							.addEntry("Comment",
									"Sleeksnap - Java Screenshot Program")
							.addEntry("Terminal", false)
							.addEntry(
									"Exec",
									FileUtils.getJavaExecutable()
											.getAbsoluteFile()
											+ " -jar \""
											+ file.getAbsolutePath() + "\"")
							.build();

					try {
						final FileWriter writer = new FileWriter(autostart);
						try {
							writer.write(contents);
						} finally {
							writer.close();
						}
					} catch (final IOException e) {
					}
				}
			}
		}
	}

	/**
	 * The binary directory to use.
	 */
	private final File binDirectory;

	/**
	 * Construct a new updater
	 */
	public Updater() {
		binDirectory = new File(Util.getWorkingDirectory(), "bin");
		binDirectory.mkdirs();
	}

	/**
	 * Apply an update from the specified URL
	 * 
	 * @param version
	 *            The version which we are downloading
	 * @param url
	 *            The URL to download from
	 * @throws IOException
	 *             If an error occurred while downloading
	 */
	public void applyUpdate(final String version, final URL url)
			throws IOException {
		// Construct the new path
		final File out = new File(binDirectory, "Sleeksnap-v" + version
				+ ".jar");

		logger.info("Creating new file...");
		// Create the new file
		out.createNewFile();

		logger.info("Downloading " + url + "...");
		// Download the new file
		download(url, out);
	}

	/**
	 * Check for updates
	 */
	public boolean checkUpdate(final UpdaterReleaseType type,
			final boolean prompt) {
		// Check for an update.
		try {
			logger.info("Checking for updates...");

			final String data = HttpUtil.executeGet(Application.UPDATE_URL
					+ type.getFeedPath());

			final JSONObject obj = new JSONObject(data);

			// Compare versions
			final String ver = obj.getString("version");

			final String[] s = ver.split("\\.");
			final int major = Integer.parseInt(s[0]), minor = Integer
					.parseInt(s[1]), patch = Integer.parseInt(s[2]);
			if (major > Version.MAJOR || major == Version.MAJOR
					&& minor > Version.MINOR || major == Version.MAJOR
					&& minor == Version.MINOR && patch > Version.PATCH) {
				logger.info("A new version is available. Current version: "
						+ Version.getVersionString() + ", new version: "
						+ major + "." + minor + "." + patch);
				if (prompt) {
					final StringBuilder message = new StringBuilder();
					message.append("There is a new version of ")
							.append(Application.NAME).append(" available!")
							.append("\n");
					message.append("Your version: ")
							.append(Version.getVersionString()).append("\n");
					message.append("Latest version: ").append(ver).append("\n");
					message.append(
							"Click OK to download it, or Cancel to be prompted the next time you start ")
							.append(Application.NAME);

					final int choice = JOptionPane.showOptionDialog(null,
							message, "Update Available",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.INFORMATION_MESSAGE, null,
							new String[] { "OK", "Cancel" }, "OK");
					if (choice == 0) {
						logger.info("User confirmed the update.");

						applyUpdate(ver, new URL(obj.getString("file")));

						return true;
					} else {
						logger.info("User declined the update.");
					}
				} else {
					logger.info("Automatically applying update...");

					applyUpdate(ver, new URL(obj.getString("file")));

					return true;
				}
			} else {
				logger.info("No updates available.");
			}
		} catch (final JSONException e) {
			logger.severe("Unable to check for update due to web service error.");
		} catch (final IOException e) {
			// Unable to update
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Download the specified file with a ProgressPanel to show progress.
	 * 
	 * @param url
	 *            The URL to download from
	 * @param file
	 *            The file to download to
	 * @throws IOException
	 *             If a problem occurred while starting the download
	 */
	public void download(final URL url, final File file) throws IOException {
		final JFrame frame = new JFrame("Sleeksnap Update");
		final ProgressPanel panel = new ProgressPanel();
		frame.add(panel);
		frame.pack();
		frame.setResizable(false);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		try {
			frame.setIconImage(ImageIO.read(Util
					.getResourceByName("/icon32x32.png")));
		} catch (final IOException e1) {
			e1.printStackTrace();
		}

		Util.centerFrame(frame);

		final Downloader downloader = new Downloader(url, new FileOutputStream(
				file));
		downloader.addListener(panel);
		downloader.addListener(new DownloadAdapter() {
			@Override
			public void downloadFinished(final Downloader downloader) {
				updateFinished(file);
			}
		});
		downloader.start();
	}

	/**
	 * Called when the update is finished to re-launch Sleeksnap
	 * 
	 * @param file
	 *            The newly downloaded jar file
	 */
	public void updateFinished(final File file) {
		logger.info("Checking autostart...");
		verifyAutostart(file, VerificationMode.VERIFY);

		if (Platform.isWindows()) {
			logger.info("Checking start menu icon...");
			try {
				WindowsUpdater.checkStartMenu(file);
			} catch (final Exception e) {
				logger.log(Level.WARNING, "Unable to update start menu icon", e);
			}
		}

		logger.info("Launching new file...");
		try {
			Launcher.launch(file, Launcher.class.getName());

			System.exit(0);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
