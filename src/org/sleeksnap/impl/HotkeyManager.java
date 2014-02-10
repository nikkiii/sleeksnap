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
package org.sleeksnap.impl;

import java.awt.TrayIcon;

import javax.swing.KeyStroke;

import org.json.JSONException;
import org.json.JSONObject;
import org.sleeksnap.ScreenSnapper;

import com.sun.jna.Platform;
import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.HotkeyProvider;

/**
 * A simple hotkey class which uses JKeyMaster, wrapped in a few functions to
 * clean it up and give us better control
 * 
 * @author Nikki
 * 
 */
public class HotkeyManager {

	/**
	 * Active window hotkeys, for Linux and Windows only
	 */
	public static final String ACTIVE_HOTKEY = "alt PRINTSCREEN",
			ACTIVE_HOTKEY_MAC = "";

	/**
	 * Clipboard hotkeys
	 */
	public static final String CLIPBOARD_HOTKEY = "ctrl PAGE_UP",
			CLIPBOARD_HOTKEY_MAC = "ctrl shift 3";

	/**
	 * Crop shot hotkeys
	 */
	public static final String CROP_HOTKEY = "ctrl PRINTSCREEN",
			CROP_HOTKEY_MAC = "ctrl shift 2";

	/**
	 * File upload hotkeys
	 */
	public static final String FILE_HOTKEY = "ctrl PAGE_DOWN",
			FILE_HOTKEY_MAC = "ctrl shift 4";

	/**
	 * Full screenshot hotkeys
	 */
	public static final String FULL_HOTKEY = "PRINTSCREEN",
			FULL_HOTKEY_MAC = "ctrl shift 1";

	/**
	 * Settings gui hotkeys
	 */
	public static final String OPTIONS_HOTKEY = "ctrl alt PRINTSCREEN",
			OPTIONS_HOTKEY_MAC = "ctrl shift 9";

	/**
	 * Whether the input has been initialized or disabled
	 */
	private boolean initialized = false;

	/**
	 * The hotkey provider
	 */
	private final HotkeyProvider provider;

	/**
	 * The screensnapper instance
	 */
	private final ScreenSnapper snapper;

	/**
	 * Construct a new hotkey manager
	 * 
	 * @param snapper
	 *            The screensnapper instance
	 */
	public HotkeyManager(final ScreenSnapper snapper) {
		this.snapper = snapper;
		provider = HotkeyProvider.getCurrentProvider(false);
		// Register a shutdown hook just in case
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				cleanupInput();
			}
		});
	}

	/**
	 * Clean up the input bindings
	 */
	public void cleanupInput() {
		resetKeys();
		provider.stop();
	}

	/**
	 * Get whether this class has been initialized/keys have been bound
	 * 
	 * @return The value of <code>initialized</code>
	 */
	public boolean hasKeysBound() {
		return initialized;
	}

	/**
	 * Initialize the input using JKeymaster
	 * 
	 * @throws JSONException
	 */
	public void initializeInput() {
		final JSONObject keys = snapper.getConfiguration().getJSONObject(
				"hotkeys");
		// A little sanity check, just in case we don't have ANY keys set
		if (keys == null) {
			return;
		}
		if (keys.has("crop")) {
			provider.register(KeyStroke.getKeyStroke(keys.getString("crop")),
					new HotKeyListener() {
						@Override
						public void onHotKey(final HotKey hotKey) {
							snapper.crop();
						}
					});
		}
		if (keys.has("full")) {
			provider.register(KeyStroke.getKeyStroke(keys.getString("full")),
					new HotKeyListener() {
						@Override
						public void onHotKey(final HotKey hotKey) {
							snapper.full();
						}
					});
		}
		if (keys.has("clipboard")) {
			provider.register(
					KeyStroke.getKeyStroke(keys.getString("clipboard")),
					new HotKeyListener() {
						@Override
						public void onHotKey(final HotKey hotKey) {
							snapper.clipboard();
						}
					});
		}
		if (keys.has("file")) {
			provider.register(KeyStroke.getKeyStroke(keys.getString("file")),
					new HotKeyListener() {
						@Override
						public void onHotKey(final HotKey hotKey) {
							snapper.selectFile();
						}
					});
		}
		if (keys.has("options")) {
			provider.register(
					KeyStroke.getKeyStroke(keys.getString("options")),
					new HotKeyListener() {
						@Override
						public void onHotKey(final HotKey hotKey) {
							if (!snapper.openSettings()) {
								snapper.getTrayIcon()
										.displayMessage(
												"Error",
												"Could not open settings, is there another window open?",
												TrayIcon.MessageType.ERROR);
							}
						}
					});
		}
		// We support active windows only on windows/linux, but OSX SOON!
		if ((Platform.isWindows() || Platform.isLinux()) && keys.has("active")) {
			provider.register(KeyStroke.getKeyStroke(keys.getString("active")),
					new HotKeyListener() {
						@Override
						public void onHotKey(final HotKey hotKey) {
							snapper.active();
						}
					});
		}
		initialized = true;
	}

	/**
	 * Reset the bound keys and let other classes know that they are not set
	 */
	public void resetKeys() {
		provider.reset();
		initialized = false;
	}
}
