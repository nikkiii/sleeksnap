package org.sleeksnap.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

public class WinRegistry {

	public static final int HKEY_CURRENT_USER = 0x80000001;

	public static final int HKEY_LOCAL_MACHINE = 0x80000002;
	private static final int KEY_ALL_ACCESS = 0xf003f;
	private static final int KEY_READ = 0x20019;
	public static final int REG_ACCESSDENIED = 5;
	public static final int REG_NOTFOUND = 2;

	public static final int REG_SUCCESS = 0;
	private static Method regCloseKey = null;
	private static Method regCreateKeyEx = null;
	private static Method regDeleteKey = null;
	private static Method regDeleteValue = null;
	private static Method regEnumKeyEx = null;
	private static Method regEnumValue = null;
	private static Method regOpenKey = null;
	private static Method regQueryInfoKey = null;
	private static Method regQueryValueEx = null;
	private static Method regSetValueEx = null;
	public static final String RUN_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
	private static Preferences systemRoot = Preferences.systemRoot();
	private static Preferences userRoot = Preferences.userRoot();
	private static Class<? extends Preferences> userClass = userRoot.getClass();

	static {
		try {
			regOpenKey = userClass.getDeclaredMethod("WindowsRegOpenKey",
					new Class[] { int.class, byte[].class, int.class });
			regOpenKey.setAccessible(true);
			regCloseKey = userClass.getDeclaredMethod("WindowsRegCloseKey",
					new Class[] { int.class });
			regCloseKey.setAccessible(true);
			regQueryValueEx = userClass.getDeclaredMethod(
					"WindowsRegQueryValueEx", new Class[] { int.class,
							byte[].class });
			regQueryValueEx.setAccessible(true);
			regEnumValue = userClass.getDeclaredMethod("WindowsRegEnumValue",
					new Class[] { int.class, int.class, int.class });
			regEnumValue.setAccessible(true);
			regQueryInfoKey = userClass.getDeclaredMethod(
					"WindowsRegQueryInfoKey1", new Class[] { int.class });
			regQueryInfoKey.setAccessible(true);
			regEnumKeyEx = userClass.getDeclaredMethod("WindowsRegEnumKeyEx",
					new Class[] { int.class, int.class, int.class });
			regEnumKeyEx.setAccessible(true);
			regCreateKeyEx = userClass.getDeclaredMethod(
					"WindowsRegCreateKeyEx", new Class[] { int.class,
							byte[].class });
			regCreateKeyEx.setAccessible(true);
			regSetValueEx = userClass.getDeclaredMethod("WindowsRegSetValueEx",
					new Class[] { int.class, byte[].class, byte[].class });
			regSetValueEx.setAccessible(true);
			regDeleteValue = userClass.getDeclaredMethod(
					"WindowsRegDeleteValue", new Class[] { int.class,
							byte[].class });
			regDeleteValue.setAccessible(true);
			regDeleteKey = userClass.getDeclaredMethod("WindowsRegDeleteKey",
					new Class[] { int.class, byte[].class });
			regDeleteKey.setAccessible(true);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a key
	 * 
	 * @param hkey
	 *            HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
	 * @param key
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void createKey(final int hkey, final String key)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		int[] ret;
		if (hkey == HKEY_LOCAL_MACHINE) {
			ret = createKey(systemRoot, hkey, key);
			regCloseKey
					.invoke(systemRoot, new Object[] { new Integer(ret[0]) });
		} else if (hkey == HKEY_CURRENT_USER) {
			ret = createKey(userRoot, hkey, key);
			regCloseKey.invoke(userRoot, new Object[] { new Integer(ret[0]) });
		} else {
			throw new IllegalArgumentException("hkey=" + hkey);
		}
		if (ret[1] != REG_SUCCESS) {
			throw new IllegalArgumentException("rc=" + ret[1] + "  key=" + key);
		}
	}

	private static int[] createKey(final Preferences root, final int hkey,
			final String key) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		return (int[]) regCreateKeyEx.invoke(root, new Object[] {
				new Integer(hkey), toCstr(key) });
	}

	/**
	 * Delete a given key
	 * 
	 * @param hkey
	 * @param key
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void deleteKey(final int hkey, final String key)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		int rc = -1;
		if (hkey == HKEY_LOCAL_MACHINE) {
			rc = deleteKey(systemRoot, hkey, key);
		} else if (hkey == HKEY_CURRENT_USER) {
			rc = deleteKey(userRoot, hkey, key);
		}
		if (rc != REG_SUCCESS) {
			throw new IllegalArgumentException("rc=" + rc + "  key=" + key);
		}
	}

	private static int deleteKey(final Preferences root, final int hkey,
			final String key) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		final int rc = ((Integer) regDeleteKey.invoke(root, new Object[] {
				new Integer(hkey), toCstr(key) })).intValue();
		return rc; // can REG_NOTFOUND, REG_ACCESSDENIED, REG_SUCCESS
	}

	/**
	 * delete a value from a given key/value name
	 * 
	 * @param hkey
	 * @param key
	 * @param value
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void deleteValue(final int hkey, final String key,
			final String value) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		int rc = -1;
		if (hkey == HKEY_LOCAL_MACHINE) {
			rc = deleteValue(systemRoot, hkey, key, value);
		} else if (hkey == HKEY_CURRENT_USER) {
			rc = deleteValue(userRoot, hkey, key, value);
		}
		if (rc != REG_SUCCESS) {
			throw new IllegalArgumentException("rc=" + rc + "  key=" + key
					+ "  value=" + value);
		}
	}

	private static int deleteValue(final Preferences root, final int hkey,
			final String key, final String value)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		final int[] handles = (int[]) regOpenKey.invoke(root, new Object[] {
				new Integer(hkey), toCstr(key), new Integer(KEY_ALL_ACCESS) });
		if (handles[1] != REG_SUCCESS) {
			return handles[1]; // can be REG_NOTFOUND, REG_ACCESSDENIED
		}
		final int rc = ((Integer) regDeleteValue.invoke(root, new Object[] {
				new Integer(handles[0]), toCstr(value) })).intValue();
		regCloseKey.invoke(root, new Object[] { new Integer(handles[0]) });
		return rc;
	}

	/**
	 * Read a value from key and value name
	 * 
	 * @param hkey
	 *            HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
	 * @param key
	 * @param valueName
	 * @return the value
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static String readString(final int hkey, final String key,
			final String valueName) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		if (hkey == HKEY_LOCAL_MACHINE) {
			return readString(systemRoot, hkey, key, valueName);
		} else if (hkey == HKEY_CURRENT_USER) {
			return readString(userRoot, hkey, key, valueName);
		} else {
			throw new IllegalArgumentException("hkey=" + hkey);
		}
	}

	private static String readString(final Preferences root, final int hkey,
			final String key, final String value)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		final int[] handles = (int[]) regOpenKey.invoke(root, new Object[] {
				new Integer(hkey), toCstr(key), new Integer(KEY_READ) });
		if (handles[1] != REG_SUCCESS) {
			return null;
		}
		final byte[] valb = (byte[]) regQueryValueEx.invoke(root, new Object[] {
				new Integer(handles[0]), toCstr(value) });
		regCloseKey.invoke(root, new Object[] { new Integer(handles[0]) });
		return (valb != null ? new String(valb).trim() : null);
	}

	// =====================

	/**
	 * Read the value name(s) from a given key
	 * 
	 * @param hkey
	 *            HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
	 * @param key
	 * @return the value name(s)
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static List<String> readStringSubKeys(final int hkey,
			final String key) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		if (hkey == HKEY_LOCAL_MACHINE) {
			return readStringSubKeys(systemRoot, hkey, key);
		} else if (hkey == HKEY_CURRENT_USER) {
			return readStringSubKeys(userRoot, hkey, key);
		} else {
			throw new IllegalArgumentException("hkey=" + hkey);
		}
	}

	private static List<String> readStringSubKeys(final Preferences root,
			final int hkey, final String key) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		final List<String> results = new ArrayList<String>();
		final int[] handles = (int[]) regOpenKey.invoke(root, new Object[] {
				new Integer(hkey), toCstr(key), new Integer(KEY_READ) });
		if (handles[1] != REG_SUCCESS) {
			return null;
		}
		final int[] info = (int[]) regQueryInfoKey.invoke(root,
				new Object[] { new Integer(handles[0]) });

		final int count = info[2]; // count
		final int maxlen = info[3]; // value length max
		for (int index = 0; index < count; index++) {
			final byte[] name = (byte[]) regEnumKeyEx.invoke(root,
					new Object[] { new Integer(handles[0]), new Integer(index),
							new Integer(maxlen + 1) });
			results.add(new String(name).trim());
		}
		regCloseKey.invoke(root, new Object[] { new Integer(handles[0]) });
		return results;
	}

	/**
	 * Read value(s) and value name(s) form given key
	 * 
	 * @param hkey
	 *            HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
	 * @param key
	 * @return the value name(s) plus the value(s)
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static Map<String, String> readStringValues(final int hkey,
			final String key) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		if (hkey == HKEY_LOCAL_MACHINE) {
			return readStringValues(systemRoot, hkey, key);
		} else if (hkey == HKEY_CURRENT_USER) {
			return readStringValues(userRoot, hkey, key);
		} else {
			throw new IllegalArgumentException("hkey=" + hkey);
		}
	}

	private static Map<String, String> readStringValues(final Preferences root,
			final int hkey, final String key) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		final HashMap<String, String> results = new HashMap<String, String>();
		final int[] handles = (int[]) regOpenKey.invoke(root, new Object[] {
				new Integer(hkey), toCstr(key), new Integer(KEY_READ) });
		if (handles[1] != REG_SUCCESS) {
			return null;
		}
		final int[] info = (int[]) regQueryInfoKey.invoke(root,
				new Object[] { new Integer(handles[0]) });

		final int count = info[2]; // count
		final int maxlen = info[3]; // value length max
		for (int index = 0; index < count; index++) {
			final byte[] name = (byte[]) regEnumValue.invoke(root,
					new Object[] { new Integer(handles[0]), new Integer(index),
							new Integer(maxlen + 1) });
			final String value = readString(hkey, key, new String(name));
			results.put(new String(name).trim(), value);
		}
		regCloseKey.invoke(root, new Object[] { new Integer(handles[0]) });
		return results;
	}

	// utility
	private static byte[] toCstr(final String str) {
		final byte[] result = new byte[str.length() + 1];

		for (int i = 0; i < str.length(); i++) {
			result[i] = (byte) str.charAt(i);
		}
		result[str.length()] = 0;
		return result;
	}

	/**
	 * Write a value in a given key/value name
	 * 
	 * @param hkey
	 * @param key
	 * @param valueName
	 * @param value
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
	public static void writeStringValue(final int hkey, final String key,
			final String valueName, final String value)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		if (hkey == HKEY_LOCAL_MACHINE) {
			writeStringValue(systemRoot, hkey, key, valueName, value);
		} else if (hkey == HKEY_CURRENT_USER) {
			writeStringValue(userRoot, hkey, key, valueName, value);
		} else {
			throw new IllegalArgumentException("hkey=" + hkey);
		}
	}

	private static void writeStringValue(final Preferences root,
			final int hkey, final String key, final String valueName,
			final String value) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		final int[] handles = (int[]) regOpenKey.invoke(root, new Object[] {
				new Integer(hkey), toCstr(key), new Integer(KEY_ALL_ACCESS) });

		regSetValueEx.invoke(root, new Object[] { new Integer(handles[0]),
				toCstr(valueName), toCstr(value) });
		regCloseKey.invoke(root, new Object[] { new Integer(handles[0]) });
	}

	private WinRegistry() {
	}
}