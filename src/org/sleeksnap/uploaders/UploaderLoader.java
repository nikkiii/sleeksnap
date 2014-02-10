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
package org.sleeksnap.uploaders;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.swing.JOptionPane;

import org.sleeksnap.ScreenSnapper;
import org.sleeksnap.util.Util;

/**
 * Loads uploaders from the uploaders plugin directory
 * 
 * @author Nikki
 * 
 */
public class UploaderLoader {

	/**
	 * The Screenshot program instance
	 */
	private final ScreenSnapper snapper;

	public UploaderLoader(final ScreenSnapper snapper) {
		this.snapper = snapper;
	}

	/**
	 * Scan the uploader directory for either class files or jar files
	 */
	public void load() throws Exception {
		// Load custom uploaders
		final File dir = new File(Util.getWorkingDirectory(),
				"plugins/uploaders");
		if (!dir.exists()) {
			dir.mkdirs();
		}

		final ClassLoader fileLoader = new URLClassLoader(new URL[] { dir
				.toURI().toURL() });
		for (final File f : dir.listFiles()) {
			final String name = f.getName();
			if (name.endsWith(".class") && !name.contains("$")) {
				loadSingleClassUploader(fileLoader, f);
			} else if (name.endsWith(".jar")) {
				loadPackedUploader(f);
			}
		}
	}

	/**
	 * Load a packed uploader jar
	 * 
	 * @param file
	 *            The file to load
	 */
	private void loadPackedUploader(final File file) {
		final String name = file.getName();
		try {
			final JarFile jar = new JarFile(file);

			final ClassLoader loader = new URLClassLoader(new URL[] { file
					.toURI().toURL() });

			final Manifest manifest = jar.getManifest();

			if (manifest != null) {
				final Attributes attrs = manifest.getMainAttributes();

				final String uploaderClass = attrs
						.getValue(Attributes.Name.MAIN_CLASS);

				if (uploaderClass == null) {
					throw new Exception("Unable to find Main-Class attribute");
				}

				// Attempt to load it

				final Class<?> c = loader.loadClass(uploaderClass);

				final Uploader<?> uploader = (Uploader<?>) c.newInstance();

				if (uploader == null) {
					throw new Exception();
				}

				snapper.registerUploader(uploader);
			} else {
				throw new Exception("Unable to find Manifest file");
			}

			jar.close();
		} catch (final Exception e) {
			JOptionPane.showMessageDialog(null,
					"An exception occured when loading " + name + " : " + e
							+ ", it could be outdated.",
					"Could not load uploader : " + name,
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Load a single uploader class
	 * 
	 * @param loader
	 *            The class loader for the directory
	 * @param file
	 *            The file to load
	 */
	private void loadSingleClassUploader(final ClassLoader loader,
			final File file) {
		final String name = file.getName().replaceAll(".class", "");
		try {
			final Class<?> c = loader.loadClass(name);

			final Uploader<?> uploader = (Uploader<?>) c.newInstance();

			if (uploader == null) {
				throw new Exception();
			}

			snapper.registerUploader(uploader);
		} catch (final Exception e) {
			JOptionPane.showMessageDialog(null,
					"An exception occured when loading " + name + " : " + e
							+ ", it could be outdated.",
					"Could not load uploader : " + name,
					JOptionPane.ERROR_MESSAGE);
		}
	}
}
