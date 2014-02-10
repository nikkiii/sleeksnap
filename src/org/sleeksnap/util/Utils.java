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

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.xml.bind.DatatypeConverter;

import org.sleeksnap.Constants.Application;
import org.sleeksnap.upload.FileUpload;
import org.sleeksnap.upload.ImageUpload;
import org.sleeksnap.upload.TextUpload;
import org.sleeksnap.upload.Upload;
import org.sleeksnap.uploaders.Uploader;

import com.sun.jna.Platform;

/**
 * A class containing many utility classes which would be wasteful to put in a
 * real class.
 * 
 * @author Nikki
 * 
 */
public class Utils {

	/**
	 * A class which provides functions related to the use of Class elements
	 * 
	 * @author Nikki
	 * 
	 */
	public static class ClassUtil {

		/**
		 * Format a class name to get only the real name, not the package
		 * 
		 * @param clazz
		 *            The class
		 * @return The name
		 */
		public static String formatName(final Class<?> clazz) {
			String name = clazz.getName();
			final int packageIndex = name.lastIndexOf(".");
			if (packageIndex != -1) {
				name = name.substring(packageIndex + 1);
			}
			final int subIndex = name.lastIndexOf("$");
			if (subIndex != -1) {
				name = name.substring(0, subIndex);
			}
			return name;
		}
	}

	/**
	 * A basic class to use clipboard related functions
	 * 
	 * @author Nikki
	 * 
	 */
	public static class ClipboardUtil {
		@SuppressWarnings("serial")
		public static class ClipboardException extends Exception {
			/**
			 * 
			 */
			private static final long serialVersionUID = -7837442243903114379L;

			public ClipboardException(final Exception e) {
				super(e);
			}
		}

		/**
		 * The clipboard instance
		 */
		private static Clipboard clipboard = Toolkit.getDefaultToolkit()
				.getSystemClipboard();

		@SuppressWarnings("unchecked")
		/**
		 * Get the clipboard contents
		 * @return
		 * 			Either an Image, String or File
		 * @throws ClipboardException
		 * 			If an error occurred
		 */
		public static Object getClipboardContents() throws ClipboardException {
			final Transferable contents = clipboard.getContents(null);
			if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
				// An image is on the clipboard!
				try {
					// Get the image from the clipboard
					final Image image = (Image) contents
							.getTransferData(DataFlavor.imageFlavor);
					if (image != null) {
						return image;
					}
				} catch (final Exception e) {
					throw new ClipboardException(e);
				}
			} else if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				// Text is on the clipboard!
				try {
					// Get the string from the clipboard
					final String string = (String) contents
							.getTransferData(DataFlavor.stringFlavor);
					if (string != null) {
						return string;
					}
				} catch (final Exception e) {
					throw new ClipboardException(e);
				}
			} else if (contents
					.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				// Files are on the clipboard
				try {
					// We only will use the first file...
					final List<File> list = (List<File>) contents
							.getTransferData(DataFlavor.javaFileListFlavor);
					if (list.size() > 0) {
						return list.get(0);
					}
				} catch (final Exception e) {
					throw new ClipboardException(e);
				}
			}
			return null;
		}

		/**
		 * Set the clipboard contents to a string
		 * 
		 * @param string
		 *            The contents
		 */
		public static void setClipboard(final String string) {
			clipboard.setContents(new StringSelection(string), null);
		}
	}

	/**
	 * A utility class used for formatting Dates
	 * 
	 * @author Nikki
	 * 
	 */
	public static class DateUtil {

		/**
		 * The date format
		 */
		private static SimpleDateFormat dateFormat = new SimpleDateFormat(
				"MM-dd-yyyy_HH-mm-ss");

		/**
		 * Gets a filename friendly date
		 * 
		 * @return The formatted date
		 */
		public static String getCurrentDate() {
			return dateFormat.format(new Date());
		}
	}

	/**
	 * Contains display methods, will use more later...
	 * 
	 * @author Nikki
	 * 
	 */
	public static class DisplayUtil {

		/**
		 * Get the real screen size, multiple screens..
		 * 
		 * @return The screen size
		 */
		public static Rectangle[] getAllScreenBounds() {
			final GraphicsEnvironment ge = GraphicsEnvironment
					.getLocalGraphicsEnvironment();
			final GraphicsDevice[] screens = ge.getScreenDevices();

			final Rectangle[] allScreenBounds = new Rectangle[screens.length];
			for (int i = 0; i < screens.length; i++) {
				allScreenBounds[i] = screens[i].getDefaultConfiguration()
						.getBounds();
			}
			return allScreenBounds;
		}

		/**
		 * Get the real screen size, multiple screens..
		 * 
		 * @return The screen size
		 */
		public static Rectangle getRealScreenSize() {
			final GraphicsEnvironment ge = GraphicsEnvironment
					.getLocalGraphicsEnvironment();
			final GraphicsDevice[] screens = ge.getScreenDevices();

			final Rectangle allScreenBounds = new Rectangle();
			for (final GraphicsDevice screen : screens) {
				final Rectangle screenBounds = screen.getDefaultConfiguration()
						.getBounds();

				allScreenBounds.width += screenBounds.width;
				allScreenBounds.height = Math.max(allScreenBounds.height,
						screenBounds.height);
			}
			return allScreenBounds;
		}
	}

	/**
	 * A class containing simple file utilities
	 * 
	 * @author Nikki
	 * 
	 */
	public static class FileUtils {

		/**
		 * Generate a unique filename for uploads etc
		 * 
		 * @param extension
		 *            The file extension
		 * @return The generated filename
		 */
		public static String generateFileName(final String extension) {
			return Application.NAME + "-" + DateUtil.getCurrentDate()
					+ (extension.equals("") ? "" : "." + extension);
		}

		/**
		 * Generate a file name from a date object formatted for filenames, plus
		 * the filename if applicable
		 * 
		 * @param upload
		 *            The object to be uploaded
		 * @return The filename
		 */
		public static String generateFileName(final Upload upload) {
			String name = DateUtil.getCurrentDate();
			if (upload instanceof ImageUpload) {
				name += ".png";
			} else if (upload instanceof TextUpload) {
				name += ".txt";
			} else if (upload instanceof FileUpload) {
				name = ((FileUpload) upload).getFile().getName();
			} else {
				name += ".file";
			}
			return name;
		}

		/**
		 * Get the jar running from as a File object
		 * 
		 * @param cl
		 *            The class to get the path from
		 * @return A File object representing the path
		 * @throws Exception
		 *             If the path is invalid
		 */
		public static File getJarFile(final Class<?> cl) throws Exception {
			return new File(cl.getProtectionDomain().getCodeSource()
					.getLocation().toURI());
		}

		/**
		 * Get the jar path
		 * 
		 * @param cl
		 *            The class to get the path from
		 * @return The path
		 * @throws Exception
		 *             If the path is invalid
		 */
		public static String getJarPath(final Class<?> cl) throws Exception {
			return getJarFile(cl).getAbsolutePath();
		}

		/**
		 * Find the java executable for the current java runtime
		 * 
		 * @return The File of the java executable, or null if it was not found
		 *         (This is very bad)
		 */
		public static File getJavaExecutable() {
			final File javaDir = new File(System.getProperty("java.home"));

			final String[] exes = new String[] { "bin/javaw", "bin/java" };

			final boolean isWindows = Platform.isWindows();

			for (String s : exes) {
				if (isWindows) {
					s = s + ".exe";
				}
				final File f = new File(javaDir, s);
				if (f.exists()) {
					return f;
				}
			}
			return null;
		}

		/**
		 * Get the mime type for a file
		 * 
		 * @param fileUrl
		 *            The file url (Can use file.toURI().toURL())
		 * @return The mime type
		 */
		public static String getMimeType(final String fileUrl) {
			final FileNameMap fileNameMap = URLConnection.getFileNameMap();
			final String type = fileNameMap.getContentTypeFor(fileUrl);

			return type;
		}

		/**
		 * Read a file
		 * 
		 * @param file
		 *            The file
		 * @return The contents
		 * @throws IOException
		 *             If an error occurred
		 */
		public static String readFile(final File file) throws IOException {
			final StringBuilder contents = new StringBuilder();
			final BufferedReader reader = new BufferedReader(new FileReader(
					file));
			try {
				while (true) {
					final String line = reader.readLine();
					if (line == null) {
						break;
					}
					contents.append(line).append("\n");
				}
			} finally {
				reader.close();
			}
			return contents.toString();
		}
	}

	/**
	 * Formatting utils
	 * 
	 * @author Nikki
	 * 
	 */
	public static class FormatUtil {

		/**
		 * Parse a formatted 'expiration' time (for example 6 hours) to minutes.
		 * 
		 * @param str
		 *            The input string
		 * @return The time parsed, defaults to 0 if unable to parse.
		 */
		public static int formattedTimeToMinutes(final String str) {
			int mod = 0;
			if (str.endsWith("hour") || str.endsWith("hours")) {
				mod = 60;
			} else if (str.endsWith("day") || str.endsWith("days")) {
				mod = 1440;
			} else if (str.endsWith("month") || str.endsWith("months")) {
				mod = 43200;
			} else if (str.endsWith("year") || str.endsWith("years")) {
				mod = 525600;
			}
			int res = 0;
			if (mod != 0) {
				res = Integer.parseInt(str.substring(0, str.indexOf(' ')))
						* mod;
			} else {
				try {
					res = Integer.parseInt(str);
				} catch (final NumberFormatException e) {
					// Default.
				}
			}
			return res;
		}

		/**
		 * Parse a formatted 'expiration' time (for example 6 hours) to seconds.
		 * 
		 * @param str
		 *            The input string
		 * @return The time parsed, defaults to 0 if unable to parse.
		 */
		public static int formattedTimeToSeconds(final String str) {
			return formattedTimeToMinutes(str) * 60;
		}

		/**
		 * Convert a byte count to a human readable count
		 * 
		 * @param bytes
		 *            The input bytes
		 * @param si
		 *            false = binary, true = si
		 * @return The result of the format
		 */
		public static String humanReadableByteCount(final long bytes,
				final boolean si) {
			final int unit = si ? 1000 : 1024;
			if (bytes < unit) {
				return bytes + " B";
			}
			final int exp = (int) (Math.log(bytes) / Math.log(unit));
			final String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1)
					+ (si ? "" : "i");
			return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
		}

		/**
		 * Pad a number less than 10 with a zero
		 * 
		 * @param num
		 *            The number to pad
		 * @return The padded number
		 */
		public static String padZeros(final int num) {
			if (num >= 0 && num <= 9) {
				return "0" + num;
			}
			return Integer.toString(num);
		}

		/**
		 * Format time elapsed
		 * 
		 * @param time
		 *            The time elapsed (In milliseconds)
		 * @param pad
		 *            Whether to pad with zeros
		 * @return The formatted string
		 */
		public static String timeElapsed(long time, final boolean pad) {
			time = time / 1000;
			final int hours = (int) (time / 3600), minutes = (int) ((time % 3600) / 60), seconds = (int) (time % 60);
			final StringBuilder bldr = new StringBuilder();
			if (hours > 0) {
				bldr.append(pad ? padZeros(hours) : hours).append(
						" hour" + (hours == 1 ? "" : "s"));
			}
			if (bldr.length() > 0) {
				bldr.append(", ");
			}
			if (minutes > 0) {
				bldr.append(pad ? padZeros(minutes) : minutes).append(
						" minute" + (minutes == 1 ? "" : "s"));
			}
			if (bldr.length() > 0) {
				bldr.append(" and ");
			}
			bldr.append(pad ? padZeros(seconds) : seconds).append(
					" second" + (seconds == 1 ? "" : "s"));

			return bldr.toString();
		}
	}

	/**
	 * A basic class to hold Image related functions
	 * 
	 * @author Nikki
	 * 
	 */
	public static class ImageUtil {

		/**
		 * Check whether an image has alpha pixels
		 * 
		 * @param image
		 *            The image
		 * @return True, if the image has alpha pixels
		 */
		public static boolean hasAlpha(final Image image) {
			// If buffered image, the color model is readily available
			if (image instanceof BufferedImage) {
				final BufferedImage bimage = (BufferedImage) image;
				return bimage.getColorModel().hasAlpha();
			}

			// Use a pixel grabber to retrieve the image's color model;
			// grabbing a single pixel is usually sufficient
			final PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
			try {
				pg.grabPixels();
			} catch (final InterruptedException e) {
			}

			// Get the image's color model
			final ColorModel cm = pg.getColorModel();
			if (cm == null) {
				return false;
			}
			return cm.hasAlpha();
		}

		/**
		 * Convert an image into a Base64 string
		 * 
		 * @param image
		 *            The image
		 * @return The base64 encoded image data
		 * @throws IOException
		 *             If an error occurred
		 */
		public static String toBase64(final BufferedImage image)
				throws IOException {
			final ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(image, "PNG", output);
			return DatatypeConverter.printBase64Binary(output.toByteArray());
		}

		/**
		 * Convert a regular image to a buffered image
		 * 
		 * @param image
		 *            The image
		 * @return The buffered image
		 */
		public static BufferedImage toBufferedImage(Image image) {
			if (image instanceof BufferedImage) {
				return (BufferedImage) image;
			}

			// This code ensures that all the pixels in the image are loaded
			image = new ImageIcon(image).getImage();

			// Determine if the image has transparent pixels; for this method's
			// implementation, see Determining If an Image Has Transparent
			// Pixels
			final boolean hasAlpha = hasAlpha(image);

			// Create a buffered image with a format that's compatible with the
			// screen
			BufferedImage bimage = null;
			final GraphicsEnvironment ge = GraphicsEnvironment
					.getLocalGraphicsEnvironment();
			try {
				// Determine the type of transparency of the new buffered image
				int transparency = Transparency.OPAQUE;
				if (hasAlpha) {
					transparency = Transparency.BITMASK;
				}

				// Create the buffered image
				final GraphicsDevice gs = ge.getDefaultScreenDevice();
				final GraphicsConfiguration gc = gs.getDefaultConfiguration();
				bimage = gc.createCompatibleImage(image.getWidth(null),
						image.getHeight(null), transparency);
			} catch (final HeadlessException e) {
				// The system does not have a screen
			}

			if (bimage == null) {
				// Create a buffered image using the default color model
				int type = BufferedImage.TYPE_INT_RGB;
				if (hasAlpha) {
					type = BufferedImage.TYPE_INT_ARGB;
				}
				bimage = new BufferedImage(image.getWidth(null),
						image.getHeight(null), type);
			}

			// Copy image to buffered image
			final Graphics g = bimage.createGraphics();

			// Paint the image onto the buffered image
			g.drawImage(image, 0, 0, null);
			g.dispose();

			return bimage;
		}

		/**
		 * Get an image as a ByteArrayInputStream
		 * 
		 * @param image
		 *            The image
		 * @return The inputstream
		 * @throws IOException
		 *             If an error occurred while writing or reading
		 */
		public static InputStream toInputStream(final BufferedImage image)
				throws IOException {
			final ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(image, "PNG", output);
			return new ByteArrayInputStream(output.toByteArray());
		}
	}

	/**
	 * A class used for sorting
	 * 
	 * @author Nikki
	 * 
	 */
	public static class SortingUtil {
		public static List<Uploader<?>> sortUploaders(
				final Collection<Uploader<?>> uploaders) {
			final List<Uploader<?>> list = new LinkedList<Uploader<?>>(
					uploaders);
			Collections.sort(list, new Comparator<Uploader<?>>() {
				@Override
				public int compare(final Uploader<?> o1, final Uploader<?> o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			return list;
		}
	}
}
