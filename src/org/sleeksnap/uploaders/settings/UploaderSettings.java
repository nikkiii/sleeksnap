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
package org.sleeksnap.uploaders.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * A more customizable settings system with support for encrypting passwords by
 * a user-supplied master password
 * 
 * @author Nikki
 * 
 */
public class UploaderSettings {

	/**
	 * The backing JSONObject
	 */
	private JSONObject settings;

	/**
	 * Construct a blank settings instance
	 */
	public UploaderSettings() {
		settings = new JSONObject();
	}

	/**
	 * Construct a settings instance from an existing object
	 * 
	 * @param object
	 *            The object to construct from
	 */
	public UploaderSettings(final JSONObject object) {
		settings = object;
	}

	/**
	 * Get a generic Object
	 * 
	 * @param name
	 *            The object key
	 * @return The object
	 */
	public Object get(final String name) {
		return settings.get(name);
	}

	public JSONObject getBaseObject() {
		return settings;
	}

	public boolean getBoolean(final String key, final boolean defaultValue) {
		return settings.getBoolean(key, defaultValue);
	}

	public int getInt(final String key) {
		return settings.getInt(key);
	}

	public int getInt(final String key, final int defaultValue) {
		return settings.getInt(key, defaultValue);
	}

	/**
	 * Get a JSONObject value
	 * 
	 * @param string
	 *            The object key
	 * @return The JSONObject
	 */
	public JSONObject getJSONObject(final String string) {
		return settings.getJSONObject(string);
	}

	/**
	 * Get a password
	 * 
	 * TODO decrypt
	 * 
	 * @param key
	 *            The key to get
	 * @return The password
	 */
	public String getPassword(final String key) {
		if (settings.has(key)) {
			final JSONObject obj = settings.getJSONObject(key);
			if (obj.getString("type").equals("password")) {
				obj.getString("encrypted");
			}
		}
		return null;
	}

	public String getString(final String key) {
		return settings.getString(key);
	}

	public String getString(final String key, final String defaultValue) {

		if (!settings.has(key)) {
			return defaultValue;
		}

		return settings.getString(key);
	}

	public String getStringBlankDefault(final String key,
			final String defaultValue) {

		if (!settings.has(key)) {
			return defaultValue;
		}

		final String ret = settings.getString(key);

		if (ret == null || ret.trim().equals("")) {
			return defaultValue;
		}

		return ret;
	}

	public boolean has(final String name) {
		return settings.has(name);
	}

	public boolean isEmpty(final String string) {
		return settings.getString(string, "").equals("");
	}

	public void load(final File file) throws IOException {
		final FileInputStream input = new FileInputStream(file);
		try {
			settings = new JSONObject(new JSONTokener(input));
		} finally {
			input.close();
		}
	}

	public void remove(final String string) {
		settings.remove(string);
	}

	/**
	 * Save the settings to a specific file
	 * 
	 * @param file
	 *            The file to save to
	 * @throws IOException
	 *             If an error occurs
	 */
	public void save(final File file) throws IOException {
		final FileOutputStream output = new FileOutputStream(file);
		try {
			saveTo(output);
		} finally {
			output.close();
		}
	}

	/**
	 * Write the JSON to an OutputStream (Using write to an OutputStreamWriter
	 * does not work for some reason)
	 * 
	 * @param out
	 *            The output stream to write to
	 * @throws IOException
	 *             If an error occurs while writing
	 */
	public void saveTo(final OutputStream out) throws IOException {
		try {
			out.write(settings.toString(4).getBytes());
		} catch (final JSONException e) {
			e.printStackTrace();
		}
	}

	public void set(final String string, final Object value) {
		settings.put(string, value);
	}

	/**
	 * Set the JSONObject which this settings object gets it's information from
	 * 
	 * @param object
	 *            The object
	 */
	public void setBaseObject(final JSONObject object) {
		settings = object;
	}

	/**
	 * Set a password value (Encrypted by a user's password if saved to a file)
	 * 
	 * TODO encrypt it on file
	 * 
	 * @param key
	 *            The password key
	 * @param value
	 *            The password
	 */
	public void setPassword(final String key, final Object value) {
		set(key, value.toString());
	}

	/**
	 * Get this object as a JSON String
	 * 
	 * @return The JSON data
	 */
	@Override
	public String toString() {
		return settings.toString();
	}
}
