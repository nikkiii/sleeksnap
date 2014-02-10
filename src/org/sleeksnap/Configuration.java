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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Configuration {

	/**
	 * The configuration map
	 */
	private JSONObject config = new JSONObject();

	/**
	 * The file which we loaded from
	 */
	private File file;

	/**
	 * Empty constructor
	 */
	public Configuration() {

	}

	/**
	 * Check whether the configuration map contains a string
	 * 
	 * @param string
	 *            The string
	 * @return Whether it contains the string or not
	 */
	public boolean contains(final String string) {
		return config.has(string);
	}

	/**
	 * Get a generic object, auto casting
	 * 
	 * @param key
	 *            The key
	 * @return The result
	 * @throws JSONException
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(final String key) {
		return (T) config.get(key);
	}

	/**
	 * Get a boolean
	 * 
	 * @param key
	 *            The key
	 * @return The result
	 * @throws JSONException
	 */
	public boolean getBoolean(final String key) {
		return config.getBoolean(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T getEnumValue(final String key, final Class<?> class1) {
		if (class1.isEnum()) {
			return (T) class1.getEnumConstants()[getInteger(key)];
		}
		return null;
	}

	/**
	 * Get an integer
	 * 
	 * @param key
	 *            The key
	 * @return The integer
	 * @throws JSONException
	 */
	public int getInteger(final String key) {
		return config.getInt(key);
	}

	/**
	 * Get an integer
	 * 
	 * @param key
	 *            The key
	 * @return The integer
	 * @throws JSONException
	 */
	public int getInteger(final String key, final int def) {
		return config.getInt(key, def);
	}

	/**
	 * Get a map which is automatically cast to the class specified
	 * 
	 * @param key
	 *            The key
	 * @return The map attached to the specified key
	 * @throws JSONException
	 */
	public JSONObject getJSONObject(final String key) {
		return config.getJSONObject(key);
	}

	/**
	 * Get an object
	 * 
	 * @param key
	 *            The key
	 * @return The object
	 * @throws JSONException
	 */
	public Object getObject(final String key) {
		return config.get(key);
	}

	/**
	 * Get a string result
	 * 
	 * @param key
	 *            The key
	 * @return The string containing the setting for the key.
	 * @throws JSONException
	 */
	public String getString(final String key) {
		return config.getString(key);
	}

	public String getString(final String key, final String defaultValue) {
		return config.getString(key, defaultValue);
	}

	/**
	 * Load the configuration from a file
	 * 
	 * @param file
	 *            The file
	 * @throws IOException
	 *             If an error occurred while loading
	 * @throws JSONException
	 */
	public void load(final File file) throws IOException {
		this.file = file;
		try {
			final FileInputStream fInput = new FileInputStream(file);
			try {
				config = new JSONObject(new JSONTokener(fInput));
			} finally {
				fInput.close();
			}
		} catch (final RuntimeException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Load from a string
	 * 
	 * @param contents
	 *            The JSON contents
	 * @throws IOException
	 */
	public void load(final String contents) throws IOException {
		try {
			config = new JSONObject(contents);
		} catch (final RuntimeException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Put a value
	 * 
	 * @param key
	 *            The key
	 * @param value
	 *            The value
	 * @throws JSONException
	 */
	public void put(final String key, final Object value) {
		config.put(key, value);
	}

	/**
	 * Save to the original file
	 * 
	 * @throws IOException
	 *             If an error occurred
	 */
	public void save() throws IOException {
		if (file == null) {
			throw new IOException("File not set! Cannot save to file");
		}
		final FileWriter writer = new FileWriter(file);
		try {
			writer.write(config.toString(4));
		} finally {
			writer.close();
		}
	}

	/**
	 * Set the configuration file path
	 * 
	 * @param file
	 *            The file
	 */
	public void setFile(final File file) {
		this.file = file;
	}
}
