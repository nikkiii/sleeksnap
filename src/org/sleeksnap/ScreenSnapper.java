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
package org.sleeksnap;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.sleeksnap.Constants.Application;
import org.sleeksnap.Constants.Resources;
import org.sleeksnap.Constants.Version;
import org.sleeksnap.filter.FilterException;
import org.sleeksnap.filter.PNGCompressionFilter;
import org.sleeksnap.filter.UploadFilter;
import org.sleeksnap.filter.WatermarkFilter;
import org.sleeksnap.gui.OptionPanel;
import org.sleeksnap.gui.SelectionWindow;
import org.sleeksnap.impl.History;
import org.sleeksnap.impl.HistoryEntry;
import org.sleeksnap.impl.HotkeyManager;
import org.sleeksnap.impl.Language;
import org.sleeksnap.impl.LoggingManager;
import org.sleeksnap.updater.Updater;
import org.sleeksnap.updater.Updater.VerificationMode;
import org.sleeksnap.updater.UpdaterMode;
import org.sleeksnap.updater.UpdaterReleaseType;
import org.sleeksnap.upload.FileUpload;
import org.sleeksnap.upload.ImageUpload;
import org.sleeksnap.upload.TextUpload;
import org.sleeksnap.upload.URLUpload;
import org.sleeksnap.upload.Upload;
import org.sleeksnap.uploaders.Uploader;
import org.sleeksnap.uploaders.UploaderConfigurationException;
import org.sleeksnap.uploaders.UploaderLoader;
import org.sleeksnap.uploaders.files.FilebinUploader;
import org.sleeksnap.uploaders.files.UppitUploader;
import org.sleeksnap.uploaders.generic.FTPUploader;
import org.sleeksnap.uploaders.generic.GenericUploader;
import org.sleeksnap.uploaders.generic.LocalFileUploader;
import org.sleeksnap.uploaders.generic.LocalFileUploader.ImageLocalFileUploader;
import org.sleeksnap.uploaders.images.ImagebinUploader;
import org.sleeksnap.uploaders.images.ImgurUploader;
import org.sleeksnap.uploaders.images.ImmioUploader;
import org.sleeksnap.uploaders.images.PuushUploader;
import org.sleeksnap.uploaders.text.Paste2Uploader;
import org.sleeksnap.uploaders.text.PastebinUploader;
import org.sleeksnap.uploaders.text.PastebincaUploader;
import org.sleeksnap.uploaders.text.PasteeUploader;
import org.sleeksnap.uploaders.text.PastieUploader;
import org.sleeksnap.uploaders.text.SlexyUploader;
import org.sleeksnap.uploaders.text.UpasteUploader;
import org.sleeksnap.uploaders.url.GoogleShortener;
import org.sleeksnap.uploaders.url.IsgdShortener;
import org.sleeksnap.uploaders.url.PostShortner;
import org.sleeksnap.uploaders.url.TUrlShortener;
import org.sleeksnap.uploaders.url.TinyURLShortener;
import org.sleeksnap.util.ProgramOptions;
import org.sleeksnap.util.ScreenshotUtil;
import org.sleeksnap.util.Util;
import org.sleeksnap.util.Utils.ClipboardUtil;
import org.sleeksnap.util.Utils.ClipboardUtil.ClipboardException;
import org.sleeksnap.util.Utils.DateUtil;
import org.sleeksnap.util.Utils.DisplayUtil;
import org.sleeksnap.util.Utils.FileUtils;
import org.sleeksnap.util.active.WindowUtilProvider;
import org.sleeksnap.util.logging.LogPanelHandler;

import com.sun.jna.Platform;

/**
 * The main Uploader Utility class
 * 
 * @author Nikki
 * 
 */
public class ScreenSnapper {

	/**
	 * A basic class which lets us execute a custom action based on the
	 * ScreenshotAction class
	 * 
	 * @author Nikki
	 * 
	 */
	@SuppressWarnings("serial")
	private class ActionMenuItem extends MenuItem implements ActionListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4500523364683028381L;
		/**
		 * The action id
		 */
		private final int action;

		public ActionMenuItem(final String s, final int action) {
			super(s);
			this.action = action;
			addActionListener(this);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			hotkey(action);
		}
	}

	/**
	 * A wrapper for action ids
	 * 
	 * @author Nikki
	 * 
	 */
	public static class ScreenshotAction {
		public static final int ACTIVE = 4;
		private static final int CLIPBOARD = 3;
		private static final int CROP = 1;
		public static final int FILE = 5;
		private static final int FULL = 2;
	}

	/**
	 * Logging instance
	 */
	private static final Logger logger = Logger.getLogger(ScreenSnapper.class
			.getName());

	/**
	 * A basic hack for class associations -> names
	 */
	private static HashMap<Class<?>, String> names = new HashMap<Class<?>, String>();

	/**
	 * Load the names and set the useragent
	 */
	static {
		System.setProperty("http.agent", Util.getHttpUserAgent());

		names.put(ImageUpload.class, "Images");
		names.put(TextUpload.class, "Text");
		names.put(URLUpload.class, "Urls");
		names.put(FileUpload.class, "Files");
	}

	/**
	 * Get the settings file for an uploader class
	 * 
	 * @param uploader
	 *            The uploader's class
	 * @return The settings file path
	 */
	public static File getSettingsFile(final Class<?> uploader) {
		return getSettingsFile(uploader, "json");
	}

	/**
	 * Get the settings file for an uploader class
	 * 
	 * @param uploader
	 *            The uploader's class
	 * @return The settings file path
	 */
	public static File getSettingsFile(final Class<?> uploader, final String ext) {
		String name = uploader.getName();
		if (name.contains("$")) {
			name = name.substring(0, name.indexOf('$'));
		}
		final File directory = new File(Util.getWorkingDirectory(), "config");
		if (!directory.exists()) {
			directory.mkdirs();
		}
		return new File(directory, name + "." + ext);
	}

	public static void main(final String[] args) {
		// Initialize
		final ScreenSnapper instance = new ScreenSnapper();
		instance.initialize(ProgramOptions.parseSettings(args));
	}

	/**
	 * The configuration instance
	 */
	private final Configuration configuration = new Configuration();

	/**
	 * A map containing upload filters
	 */
	private final HashMap<Class<? extends Upload>, LinkedList<UploadFilter<?>>> filters = new HashMap<Class<? extends Upload>, LinkedList<UploadFilter<?>>>();

	/**
	 * The history instance
	 */
	private History history;

	/**
	 * The tray icon
	 */
	private TrayIcon icon;

	/**
	 * The hotkey manager instance
	 */
	private HotkeyManager keyManager;

	/**
	 * The last uploaded URL, used for clicking tray icon
	 */
	private String lastUrl;

	/**
	 * Defines whether the options panel is open
	 */
	private boolean optionsOpen;

	private int retries;

	/**
	 * The basic service...
	 */
	private final ExecutorService serv = Executors.newSingleThreadExecutor();

	/**
	 * A map which contains the current uploader settings
	 */
	private final HashMap<Class<? extends Upload>, Uploader<?>> uploaderAssociations = new HashMap<Class<? extends Upload>, Uploader<?>>();

	/**
	 * A map which contains uploader classes -> a list of available uploaders
	 */
	private final HashMap<Class<? extends Upload>, Map<String, Uploader<?>>> uploaders = new HashMap<Class<? extends Upload>, Map<String, Uploader<?>>>();

	/**
	 * The ExecutorService used to upload
	 */
	private final ExecutorService uploadService = Executors
			.newSingleThreadExecutor();

	/**
	 * The selection window instances
	 */
	private SelectionWindow window = null;

	/**
	 * Perform a capture of the active window
	 */
	public void active() {
		try {
			upload(new ImageUpload(ScreenshotUtil.capture(WindowUtilProvider
					.getWindowUtil().getActiveWindow().getBounds())));
		} catch (final Exception e) {
			logger.log(Level.SEVERE,
					"Unable to take the active window screenshot", e);
			showException(e);
		}
	}

	/**
	 * Clear the screenshot selection window
	 */
	public void clearWindow() {
		window = null;
	}

	/**
	 * Upload content from the clipboard
	 */
	public void clipboard() {
		try {
			final Object clipboard = ClipboardUtil.getClipboardContents();
			if (clipboard == null) {
				icon.displayMessage(Language.getString("invalidClipboard"),
						Language.getString("invalidClipboardTitle"),
						TrayIcon.MessageType.WARNING);
				return;
			}
			if (clipboard instanceof BufferedImage) {
				upload(new ImageUpload((BufferedImage) clipboard));
			} else if (clipboard instanceof File) {
				final File file = (File) clipboard;
				final String mime = FileUtils.getMimeType(file
						.getAbsolutePath());

				// A better way to upload images, it'll check the mime type!
				if (mime.startsWith("image")) {
					upload(new ImageUpload(ImageIO.read(file)));
				} else if (mime.startsWith("text")
						&& configuration.getBoolean("plainTextUpload")) {
					upload(new TextUpload(FileUtils.readFile(file)));
				} else {
					upload(new FileUpload(file));
				}
			} else if (clipboard instanceof String) {
				final String string = clipboard.toString();
				if (string
						.matches("((mailto\\:|(news|(ht|f)tp(s?))\\://){1}\\S+)")) {
					upload(new URLUpload(clipboard.toString()));
				} else {
					upload(new TextUpload(string));
				}
			}
		} catch (final ClipboardException e) {
			logger.log(Level.SEVERE, "Unable to get clipboard contents", e);
			showException(e);
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public boolean convertUploadDefinition(final JSONObject uploadConfig,
			final Class<?> originalClass, final Class<?> newClass) {
		if (uploadConfig.has(originalClass.getName())) {
			uploadConfig.put(newClass.getName(),
					uploadConfig.remove(originalClass.getName()));
			return true;
		}
		return false;
	}

	/**
	 * Perform a screenshot crop action
	 */
	public void crop() {
		if (window != null) {
			return;
		}

		window = new SelectionWindow(this, DisplayUtil.getRealScreenSize());
		window.pack();
		window.setAlwaysOnTop(true);
		window.setVisible(true);
	}

	/**
	 * Execute an upload
	 * 
	 * @param object
	 *            The object to upload
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void executeUpload(Upload object) {
		// Run the object through the filters
		if (filters.containsKey(object.getClass())) {
			for (final UploadFilter filter : filters.get(object.getClass())) {
				try {
					object = filter.filter(object);
				} catch (final FilterException e) {
					// FilterExceptions when thrown should interrupt the upload.
					showException(e, e.getErrorMessage());
					return;
				}
			}
		}
		// Then upload it
		final Uploader uploader = uploaderAssociations.get(object.getClass());
		if (uploader != null) {
			try {
				String url = uploader.upload(object);
				if (url != null) {
					if (configuration.getBoolean("shortenurls")) {
						final Uploader shortener = uploaderAssociations
								.get(URL.class);
						if (shortener != null) {
							url = shortener.upload(new URLUpload(url));
						}
					}
					if (object instanceof ImageUpload) {
						if (configuration.getBoolean("savelocal")
								&& !(uploader instanceof ImageLocalFileUploader)) {
							final FileOutputStream output = new FileOutputStream(
									getLocalFile(DateUtil.getCurrentDate()
											+ ".png"));
							try {
								ImageIO.write(
										((ImageUpload) object).getImage(),
										"png", output);
							} finally {
								output.close();
							}
						}
						((ImageUpload) object).getImage().flush();
						((ImageUpload) object).setImage(null);
					}
					url = url.trim();

					retries = 0;

					ClipboardUtil.setClipboard(url);

					lastUrl = url;
					history.addEntry(new HistoryEntry(url, uploader.getName()));
					icon.displayMessage(Language.getString("uploadComplete"),
							Language.getString("uploadedTo", url),
							TrayIcon.MessageType.INFO);
					logger.info("Upload completed, url: " + url);
				} else {
					icon.displayMessage(Language.getString("uploadFailed"),
							Language.getString("uploadFailedError"),
							TrayIcon.MessageType.ERROR);
					logger.severe("Upload failed to execute due to an unknown error");
				}
			} catch (final UploaderConfigurationException e) {
				icon.displayMessage(Language.getString("uploaderConfigError"),
						Language.getString("uploaderConfigErrorMessage"),
						TrayIcon.MessageType.ERROR);
				logger.log(Level.SEVERE, "Upload failed to execute", e);
			} catch (final Exception e) {
				// Retry until retries > max
				final StringBuilder msg = new StringBuilder(
						"The upload failed to execute: ");
				msg.append(e.getMessage());
				final int max = configuration.getInteger("max_retries",
						Constants.Configuration.DEFAULT_MAX_RETRIES);
				if (retries++ < max) {
					logger.info("Retrying upload (" + retries + " of " + max
							+ " retries)...");
					msg.append("\nRetrying...");
					upload(object);
				} else {
					msg.append("\nReached retry limit, upload aborted.");
					logger.log(Level.SEVERE,
							"Upload failed to execute, retries: " + retries, e);
					retries = 0;
				}
				icon.displayMessage(Language.getString("uploadFailed"),
						msg.toString(), TrayIcon.MessageType.ERROR);
			}
		}
	}

	/**
	 * Perform a full screenshot action
	 */
	public void full() {
		upload(new ImageUpload(ScreenshotUtil.capture(DisplayUtil
				.getRealScreenSize())));
	}

	/**
	 * Get the configuration file
	 * 
	 * @return The configuration file
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Gets a filter's parent class type
	 * 
	 * @param filter
	 * @return The class representing the filter's upload type
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends Upload> getFilterType(final UploadFilter<?> filter) {
		// Find the uploader type
		final Type[] types = filter.getClass().getGenericInterfaces();
		for (final Type type : types) {
			if (type instanceof ParameterizedType) {
				final ParameterizedType parameterizedType = (ParameterizedType) type;
				if (parameterizedType.getRawType() == UploadFilter.class) {
					return (Class<? extends Upload>) parameterizedType
							.getActualTypeArguments()[0];
				}
			}
		}
		throw new RuntimeException("Attempted to load invalid filter!");
	}

	/**
	 * Get the Hotkey Manager
	 * 
	 * @return The hotkey manager
	 */
	public HotkeyManager getKeyManager() {
		return keyManager;
	}

	/**
	 * Get the local file for image archiving
	 * 
	 * @param fileName
	 *            The file name
	 * @return The constructed File object
	 */
	public File getLocalFile(final String fileName) {
		final File dir = new File(Util.getWorkingDirectory(), "images");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return new File(dir, fileName);
	}

	/**
	 * Get the tray icon instance
	 * 
	 * @return The instance of the Tray Icon
	 */
	public TrayIcon getTrayIcon() {
		return icon;
	}

	/**
	 * Get the default uploader associations
	 * 
	 * @return The uploader associations
	 */
	public Map<Class<? extends Upload>, Uploader<?>> getUploaderAssociations() {
		return uploaderAssociations;
	}

	/**
	 * Get the uploader which is mapped to the class type
	 * 
	 * @param cl
	 *            The type
	 * @return The uploader
	 */
	public Uploader<?> getUploaderFor(final Class<?> cl) {
		return uploaderAssociations.get(cl);
	}

	/**
	 * Attempt to get the upload type from the Superclass
	 * 
	 * @param uploader
	 *            The uploader to get the type from
	 * @return The type
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends Upload> getUploaderType(
			final Uploader<? extends Upload> uploader) {
		// Find the uploader type
		final ParameterizedType parameterizedType = (ParameterizedType) uploader
				.getClass().getGenericSuperclass();
		final Type[] args = parameterizedType.getActualTypeArguments();
		if (args.length == 0) {
			throw new RuntimeException("Uploader does not include a valid type");
		}
		return (Class<? extends Upload>) args[0];
	}

	/**
	 * Check if we have an uploader for a type
	 * 
	 * @param class1
	 *            The type to check
	 * @return True, if we have an uploader
	 */
	public boolean hasUploaderFor(final Class<? extends Object> class1) {
		return uploaderAssociations.containsKey(class1);
	}

	/**
	 * Check and perform hotkeys, uses a separate service to not tie up the
	 * uploader
	 * 
	 * @param ident
	 *            The key id
	 */
	public void hotkey(final int ident) {
		serv.execute(new Runnable() {
			@Override
			public void run() {
				switch (ident) {
				case ScreenshotAction.CROP:
					crop();
					break;
				case ScreenshotAction.FULL:
					full();
					break;
				case ScreenshotAction.CLIPBOARD:
					clipboard();
					break;
				case ScreenshotAction.FILE:
					selectFile();
					break;
				case ScreenshotAction.ACTIVE:
					active();
					break;
				}
			}
		});
	}

	/**
	 * Initialize the program
	 * 
	 * @param map
	 *            Flag to reset configuration
	 */
	private void initialize(final Map<String, Object> map) {
		// Check for a configuration option
		if (map.containsKey("dir")) {
			final File file = new File(map.get("dir").toString());
			if (!file.exists()) {
				file.mkdirs();
			}
			Util.setWorkingDirectory(file);
		}
		// Verify the directory
		final File local = Util.getWorkingDirectory();
		if (!local.exists()) {
			local.mkdirs();
		}
		// Load the Release info
		final URL releaseUrl = Util
				.getResourceByName("/org/sleeksnap/release.json");
		JSONObject releaseInfo = new JSONObject();
		if (releaseUrl != null) {
			try {
				releaseInfo = new JSONObject(new JSONTokener(
						releaseUrl.openStream()));
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		// Set the UI skin
		try {
			UIManager.setLookAndFeel(releaseInfo.getString("uiClass",
					UIManager.getSystemLookAndFeelClassName()));
		} catch (final Exception e) {
			e.printStackTrace();
		}
		// Then start
		try {
			LoggingManager.configure();
		} catch (final IOException e) {
			logger.log(
					Level.WARNING,
					"Unable to configure logger, file logging and logging panel will not work.",
					e);
			JOptionPane
					.showMessageDialog(
							null,
							"Unable to configure logger, file logging and logging panel will not work.",
							"Error", JOptionPane.ERROR_MESSAGE);
		}
		logger.info("Loading plugins...");
		try {
			loadUploaders();
			loadFilters();
		} catch (final Exception e) {
			logger.log(Level.SEVERE, "Failed to load plugins!", e);
		}
		// Load the settings
		logger.info("Loading settings...");
		try {
			loadSettings(map.containsKey("resetconfig"));
		} catch (final Exception e) {
			logger.log(Level.SEVERE, "Failed to load settings!", e);
		}
		// Load the selected language
		try {
			Language.load(map.containsKey("language") ? map.get("language")
					.toString() : configuration.getString("language",
					Constants.Configuration.DEFAULT_LANGUAGE));
		} catch (final IOException e) {
			logger.log(Level.SEVERE, "Failed to load language file!", e);
		}
		// Check the update mode
		final UpdaterMode mode = configuration.getEnumValue("updateMode",
				UpdaterMode.class);
		if (mode != UpdaterMode.MANUAL) {
			final UpdaterReleaseType type = configuration.getEnumValue(
					"updateReleaseType", UpdaterReleaseType.class);
			final Updater updater = new Updater();
			if (updater.checkUpdate(type, mode == UpdaterMode.PROMPT)) {
				return;
			}
		}
		// Load the history
		logger.info("Loading history...");
		final File historyFile = new File(local, "history.json");
		history = new History(historyFile);
		if (historyFile.exists()) {
			logger.info("Using existing history file.");
			try {
				history.load();
			} catch (final Exception e) {
				logger.log(Level.WARNING, "Failed to load history", e);
			}
		} else {
			logger.info("Using new history file.");
		}
		// Validate settings
		if (!configuration.contains("hotkeys")
				|| !configuration.contains("uploaders")) {
			promptConfigurationReset();
		}
		// Register the hotkeys
		logger.info("Registering keys...");
		keyManager = new HotkeyManager(this);
		keyManager.initializeInput();
		logger.info("Opening tray icon...");
		initializeTray();
		logger.info("Ready.");
	}

	/**
	 * Initialize the tray menu
	 */
	private void initializeTray() {
		// Add uploaders from the list we loaded earlier
		final PopupMenu tray = new PopupMenu();
		// Add the action menu
		tray.add(new ActionMenuItem(Language.getString("cropupload"),
				ScreenshotAction.CROP));
		tray.add(new ActionMenuItem(Language.getString("fullupload"),
				ScreenshotAction.FULL));

		if (Platform.isWindows() || Platform.isLinux()) {
			tray.add(new ActionMenuItem(Language.getString("activeupload"),
					ScreenshotAction.ACTIVE));
		}

		tray.addSeparator();

		tray.add(new ActionMenuItem(Language.getString("clipboardupload"),
				ScreenshotAction.CLIPBOARD));
		tray.add(new ActionMenuItem(Language.getString("fileupload"),
				ScreenshotAction.FILE));

		tray.addSeparator();

		final MenuItem settings = new MenuItem(Language.getString("options"));
		settings.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (!openSettings()) {
					icon.displayMessage(Language.getString("error"),
							Language.getString("optionsOpenError"),
							TrayIcon.MessageType.ERROR);
				}
			}
		});
		tray.add(settings);

		final MenuItem exit = new MenuItem(Language.getString("exit"));
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				shutdown();
			}
		});
		tray.add(exit);

		icon = new TrayIcon(Toolkit.getDefaultToolkit()
				.getImage(Resources.ICON), Application.NAME + " v"
				+ Version.getVersionString());
		icon.setPopupMenu(tray);
		icon.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (lastUrl != null) {
					try {
						Util.openURL(new URL(lastUrl));
					} catch (final Exception e1) {
						showException(e1, "Unable to open URL");
					}
				}
			}
		});
		try {
			SystemTray.getSystemTray().add(icon);
		} catch (final AWTException e1) {
			this.showException(e1);
		}
	}

	/**
	 * Restores the default configuration
	 * 
	 * @throws IOException
	 * @throws JSONException
	 */
	private void loadDefaultConfiguration() throws IOException {

		configuration.put("plainTextUpload", false);
		configuration.put("shortenurls", false);

		final JSONObject uploaders = new JSONObject();

		// Default uploaders
		uploaders.put(ImageUpload.class.getName(),
				ImgurUploader.class.getName());
		uploaders.put(TextUpload.class.getName(),
				PasteeUploader.class.getName());
		uploaders.put(FileUpload.class.getName(),
				FilebinUploader.class.getName());
		uploaders.put(URLUpload.class.getName(),
				GoogleShortener.class.getName());

		configuration.put("uploaders", uploaders);

		final JSONObject hotkeys = new JSONObject();

		// Hotkeys
		hotkeys.put("full", Platform.isMac() ? HotkeyManager.FULL_HOTKEY_MAC
				: HotkeyManager.FULL_HOTKEY);
		hotkeys.put("crop", Platform.isMac() ? HotkeyManager.CROP_HOTKEY_MAC
				: HotkeyManager.CROP_HOTKEY);
		hotkeys.put("clipboard",
				Platform.isMac() ? HotkeyManager.CLIPBOARD_HOTKEY_MAC
						: HotkeyManager.CLIPBOARD_HOTKEY);
		hotkeys.put("options",
				Platform.isMac() ? HotkeyManager.OPTIONS_HOTKEY_MAC
						: HotkeyManager.OPTIONS_HOTKEY);
		hotkeys.put("file", Platform.isMac() ? HotkeyManager.FILE_HOTKEY_MAC
				: HotkeyManager.FILE_HOTKEY);

		if (!Platform.isMac()) {
			hotkeys.put("active", "alt PRINTSCREEN");
		}

		configuration.put("hotkeys", hotkeys);

		configuration.put("updateMode", UpdaterMode.AUTOMATIC.ordinal());

		// Save it
		configuration.save();
	}

	/**
	 * Load upload filters
	 * 
	 * @throws Exception
	 *             If an error occurred while loading
	 */
	private void loadFilters() throws Exception {
		// Register any filters

		// PNG Compression will always be done last.
		registerFilter(new PNGCompressionFilter(this));

		// Watermarks will be done after everything else too.
		registerFilter(new WatermarkFilter());

		// Load custom filters
		final File dir = new File(Util.getWorkingDirectory(), "plugins/filters");
		if (!dir.exists()) {
			dir.mkdirs();
		}
		final ClassLoader loader = new URLClassLoader(new URL[] { dir.toURI()
				.toURL() });
		for (final File f : dir.listFiles()) {
			// TODO jar files.
			final String name = f.getName();
			if (name.endsWith(".class") && !name.contains("$")) {
				try {
					final Class<?> c = loader.loadClass(f.getName().replaceAll(
							".class", ""));
					final UploadFilter<?> uploader = (UploadFilter<?>) c
							.newInstance();
					if (uploader == null) {
						throw new Exception();
					}

					registerFilter(uploader);
				} catch (final Exception e) {
					JOptionPane.showMessageDialog(null,
							Language.getString("loadingError", name, e),
							Language.getString("filterLoadError", e),
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	/**
	 * Load settings
	 */
	private void loadSettings(final boolean resetConfig) throws Exception {
		final File configFile = new File(Util.getWorkingDirectory(),
				Application.NAME.toLowerCase() + ".conf");
		if (!configFile.exists() || resetConfig) {
			configuration.setFile(configFile);
			loadDefaultConfiguration();
		} else {
			configuration.load(configFile);
		}
		if (configuration.contains("uploaders")) {
			final JSONObject uploadConfig = configuration
					.getJSONObject("uploaders");

			if (convertUploadDefinition(uploadConfig, BufferedImage.class,
					ImageUpload.class)
					|| convertUploadDefinition(uploadConfig, String.class,
							TextUpload.class)
					|| convertUploadDefinition(uploadConfig, File.class,
							FileUpload.class)
					|| convertUploadDefinition(uploadConfig, URL.class,
							URLUpload.class)) {
				logger.info("Converted upload definitions from old configuration.");
				configuration.save();
			}

			@SuppressWarnings("unchecked")
			final Iterator<Object> it$ = uploadConfig.keys();
			while (it$.hasNext()) {
				final String key = it$.next().toString();
				final String className = uploadConfig.getString(key);

				@SuppressWarnings("unchecked")
				final Class<? extends Upload> clType = (Class<? extends Upload>) Class
						.forName(key);
				if (clType != null) {
					setDefaultUploader(clType, className);
				}
			}
		}
		if (configuration.contains("startOnStartup")
				&& configuration.getBoolean("startOnStartup")) {
			// Verify that the paths match, useful for upgrading since it won't
			// open the old file.
			final File current = FileUtils.getJarFile(ScreenSnapper.class);
			if (!current.isDirectory()) {
				Updater.verifyAutostart(current, VerificationMode.INSERT);
			}
		}
	}

	/**
	 * Load the uploaders from inside the jar file and the regular file
	 * 
	 * @throws Exception
	 *             If an error occurred
	 */
	private void loadUploaders() throws Exception {
		// Generic uploaders
		registerUploader(new FTPUploader());
		registerUploader(new LocalFileUploader());
		// Image Uploaders
		registerUploader(new ImgurUploader());
		registerUploader(new ImmioUploader());
		registerUploader(new PuushUploader());
		registerUploader(new ImagebinUploader());
		// Text uploaders
		registerUploader(new Paste2Uploader());
		registerUploader(new PasteeUploader());
		registerUploader(new PastebinUploader());
		registerUploader(new PastebincaUploader());
		registerUploader(new PastieUploader());
		registerUploader(new SlexyUploader());
		registerUploader(new UpasteUploader());
		// URL Shorteners
		registerUploader(new GoogleShortener());
		registerUploader(new TinyURLShortener());
		registerUploader(new TUrlShortener());
		registerUploader(new IsgdShortener());
		registerUploader(new PostShortner());
		// File uploaders
		registerUploader(new FilebinUploader());
		registerUploader(new UppitUploader());

		// Load custom uploaders
		final UploaderLoader loader = new UploaderLoader(this);
		loader.load();
	}

	/**
	 * Load the settings for an uploader
	 * 
	 * @param uploader
	 *            The uploader
	 */
	private void loadUploaderSettings(final Uploader<?> uploader) {
		if (!uploader.hasSettings()) {
			return;
		}
		final File file = getSettingsFile(uploader.getClass());
		if (!file.exists()) {
			final File old = getSettingsFile(uploader.getClass(), "xml");
			if (old.exists()) {
				logger.info("Converting old xml style file for "
						+ uploader.getName() + " to json...");
				final Properties props = new Properties();
				try {
					final FileInputStream input = new FileInputStream(old);
					try {
						props.loadFromXML(input);
					} finally {
						input.close();
					}
					try {
						// Update the settings
						setUploaderSettings(uploader, new JSONObject(
								new JSONTokener(input)));
						// Save it
						if (uploader.hasSettings()) {
							uploader.getSettings().save(file);
						} else if (uploader.hasParent()
								&& uploader.getParentUploader().hasSettings()) {
							uploader.getParentUploader().getSettings()
									.save(file);
						}
						// If everything went well, delete the old file
						old.delete();
					} catch (final Exception e) {
						e.printStackTrace();
					}
				} catch (final IOException e) {
					// It's invalid, don't try to use it
					old.delete();
				}
			}
		} else if (file.exists()) {
			try {
				final FileInputStream input = new FileInputStream(file);
				try {
					setUploaderSettings(uploader, new JSONObject(
							new JSONTokener(input)));
				} finally {
					input.close();
				}
			} catch (final Exception e) {
				file.delete();
			}
		}
	}

	/**
	 * Open the settings panel
	 */
	public boolean openSettings() {
		if (optionsOpen) {
			return false;
		}
		optionsOpen = true;

		final JFrame frame = new JFrame("Sleeksnap Settings");

		final OptionPanel panel = new OptionPanel(this);
		panel.getUploaderPanel().setImageUploaders(
				uploaders.get(ImageUpload.class).values());
		panel.getUploaderPanel().setTextUploaders(
				uploaders.get(TextUpload.class).values());
		panel.getUploaderPanel().setURLUploaders(
				uploaders.get(URLUpload.class).values());
		panel.getUploaderPanel().setFileUploaders(
				uploaders.get(FileUpload.class).values());
		panel.setHistory(history);
		panel.doneBuilding();

		frame.add(panel);
		frame.pack();
		frame.setVisible(true);
		frame.setResizable(false);
		try {
			frame.setIconImage(ImageIO.read(Util
					.getResourceByName("/icon32x32.png")));
		} catch (final IOException e1) {
			e1.printStackTrace();
		}
		Util.centerFrame(frame);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(final WindowEvent e) {
				optionsOpen = false;
				LogPanelHandler.unbind();
			}
		});
		return true;
	}

	/**
	 * Prompt the user for a configuration reset
	 */
	public void promptConfigurationReset() {
		final int option = JOptionPane.showConfirmDialog(null,
				Language.getString("settingsCorrupted"),
				Language.getString("errorLoadingSettings"),
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (option == JOptionPane.YES_OPTION) {
			try {
				loadSettings(true);
			} catch (final Exception e) {
				logger.log(Level.SEVERE, "Unable to load default settings!", e);
			}
		} else if (option == JOptionPane.NO_OPTION) {
			// If no, let them set the configuration themselves..
			openSettings();
		} else if (option == JOptionPane.CANCEL_OPTION) {
			// Exit, they don't want anything to do with it.
			System.exit(0);
		}
	}

	/**
	 * Register an upload filter
	 * 
	 * @param filter
	 *            The filter to register
	 */
	public void registerFilter(final UploadFilter<?> filter) {
		final Class<? extends Upload> type = getFilterType(filter);
		LinkedList<UploadFilter<?>> filterList = filters.get(type);
		if (filterList == null) {
			filters.put(type, filterList = new LinkedList<UploadFilter<?>>());
		}
		filterList.addFirst(filter);
	}

	/**
	 * Register an uploader
	 * 
	 * @param uploader
	 *            The uploader
	 */
	public void registerUploader(final Uploader<?> uploader) {
		if (uploader instanceof GenericUploader) {
			final GenericUploader u = (GenericUploader) uploader;
			loadUploaderSettings(u);
			for (final Uploader<?> up : u.getSubUploaders()) {
				up.setParentUploader(u);
				registerUploader(up);
			}
			return;
		}
		final Class<? extends Upload> type = getUploaderType(uploader);
		// Check for the current list of types
		if (!uploaders.containsKey(type)) {
			uploaders.put(type, new HashMap<String, Uploader<?>>());
		}
		// Load the settings, this method should only be called once per
		// uploader, so it's the only place that is really 'right'
		loadUploaderSettings(uploader);
		uploader.onActivation();
		uploaders.get(type).put(uploader.getClass().getName(), uploader);
	}

	/**
	 * Upload a file to the file service by selecting in another window
	 */
	public void selectFile() {
		final JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		final int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			final File file = chooser.getSelectedFile();
			final int confirm = JOptionPane.showConfirmDialog(null,
					Language.getString("uploadConfirm", file.getName()),
					Language.getString("uploadConfirmTitle"),
					JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (confirm == JOptionPane.YES_OPTION) {
				JOptionPane.showMessageDialog(null,
						Language.getString("fileUploading"),
						Language.getString("fileUploadTitle"),
						JOptionPane.INFORMATION_MESSAGE);
				upload(new FileUpload(file.getAbsoluteFile()));
			} else {
				JOptionPane.showMessageDialog(null,
						Language.getString("fileUploadCanceled"),
						Language.getString("fileUploadTitle"),
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/**
	 * Set a default type's uploader, checking if the settings are valid first
	 * 
	 * @param type
	 *            The class type
	 * @param name
	 *            The uploader name
	 */
	public void setDefaultUploader(final Class<? extends Upload> type,
			final String name) {
		if (uploaders.containsKey(type)) {
			final Map<String, Uploader<? extends Upload>> map = uploaders
					.get(type);
			if (map.containsKey(name)) {
				final Uploader<? extends Upload> uploader = map.get(name);
				setDefaultUploader(uploader, false);
			} else {
				throw new RuntimeException("Invalid uploader " + name
						+ "! Possible choices: " + map.values());
			}
		} else {
			throw new RuntimeException("No uploaders set for " + type.getName());
		}
	}

	/**
	 * Set a default uploader, includes loading the settings
	 * 
	 * @param uploader
	 *            The uploader
	 * @param settingsOverride
	 *            Whether to override the settings even if required fields
	 *            aren't set
	 */
	public void setDefaultUploader(final Uploader<?> uploader,
			final boolean settingsOverride) {
		uploaderAssociations.put(getUploaderType(uploader), uploader);
	}

	private void setUploaderSettings(final Uploader<?> uploader,
			final JSONObject settings) {
		if (uploader.hasSettings()) {
			uploader.getSettings().setBaseObject(settings);
		} else if (uploader.hasParent()
				&& uploader.getParentUploader().hasSettings()) {
			uploader.getParentUploader().getSettings().setBaseObject(settings);
		}
	}

	/**
	 * Show a TrayIcon message for an exception
	 * 
	 * @param e
	 *            The exception
	 */
	public void showException(final Exception e) {
		icon.displayMessage(Language.getString("error"),
				Language.getString("exceptionCause", e.getMessage()),
				MessageType.ERROR);
	}

	/**
	 * Show a TrayIcon message for an exception
	 * 
	 * @param e
	 *            The exception
	 */
	public void showException(final Exception e, final String errorMessage) {
		icon.displayMessage(
				Language.getString("error"),
				Language.getString("exceptionCauseWithMessage", errorMessage,
						e.getMessage()), MessageType.ERROR);
	}

	/**
	 * Clean up and shut down
	 */
	private void shutdown() {
		uploadService.shutdown();
		System.exit(0);
	}

	/**
	 * Upload an object
	 * 
	 * @param upload
	 *            The upload object
	 */
	public void upload(final Upload upload) {
		// Check associations
		if (!uploaderAssociations.containsKey(upload.getClass())) {
			icon.displayMessage(Language.getString("noUploaderTitle"), Language
					.getString("noUploader", upload.getClass().getName()),
					TrayIcon.MessageType.ERROR);
			return;
		}

		uploadService.execute(new Runnable() {
			@Override
			public void run() {
				icon.setImage(Resources.ICON_BUSY_IMAGE);
				executeUpload(upload);
				icon.setImage(Resources.ICON_IMAGE);
			}
		});
	}
}
