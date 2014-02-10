/*
 * Copyright (c) 2011 Denis Tulskiy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tulskiy.keymaster.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.sun.jna.Platform;
import com.tulskiy.keymaster.osx.CarbonProvider;
import com.tulskiy.keymaster.windows.WindowsProvider;
import com.tulskiy.keymaster.x11.X11Provider;

/**
 * Main interface to global hotkey providers
 * <p/>
 * Author: Denis Tulskiy Date: 6/12/11
 */
public abstract class HotkeyProvider {
	private class HotKeyEvent implements Runnable {
		private final HotKey hotKey;

		private HotKeyEvent(final HotKey hotKey) {
			this.hotKey = hotKey;
		}

		@Override
		public void run() {
			hotKey.getListener().onHotKey(hotKey);
		}
	}

	public static final Logger logger = Logger.getLogger(HotkeyProvider.class
			.getName());

	/**
	 * Get global hotkey provider for current platform
	 * 
	 * @param useSwingEventQueue
	 *            whether the provider should be using Swing Event queue or a
	 *            regular thread
	 * @return new instance of Provider, or null if platform is not supported
	 * @see X11Provider
	 * @see WindowsProvider
	 * @see CarbonProvider
	 */
	public static HotkeyProvider getCurrentProvider(
			final boolean useSwingEventQueue) {
		HotkeyProvider provider;
		if (Platform.isX11()) {
			provider = new X11Provider();
		} else if (Platform.isWindows()) {
			provider = new WindowsProvider();
		} else if (Platform.isMac()) {
			provider = new CarbonProvider();
		} else {
			logger.warning("No suitable provider for "
					+ System.getProperty("os.name"));
			return null;
		}
		provider.setUseSwingEventQueue(useSwingEventQueue);
		provider.init();
		return provider;

	}

	private ExecutorService eventQueue;

	private boolean useSwingEventQueue;

	/**
	 * Helper method fro providers to fire hotkey event in a separate thread
	 * 
	 * @param hotKey
	 *            hotkey to fire
	 */
	protected void fireEvent(final HotKey hotKey) {
		final HotKeyEvent event = new HotKeyEvent(hotKey);
		if (useSwingEventQueue) {
			SwingUtilities.invokeLater(event);
		} else {
			if (eventQueue == null) {
				eventQueue = Executors.newSingleThreadExecutor();
			}
			eventQueue.execute(event);
		}
	}

	/**
	 * Initialize provider. Starts main thread that will listen to hotkey events
	 */
	protected abstract void init();

	/**
	 * Register a global hotkey. Only keyCode and modifiers fields are respected
	 * 
	 * @param keyCode
	 *            KeyStroke to register
	 * @param listener
	 *            listener to be notified of hotkey events
	 * @see KeyStroke
	 */
	public abstract void register(KeyStroke keyCode, HotKeyListener listener);

	/**
	 * Register a media hotkey. Currently supported media keys are:
	 * <p/>
	 * <ul>
	 * <li>Play/Pause</li>
	 * <li>Stop</li>
	 * <li>Next track</li>
	 * <li>Previous Track</li>
	 * </ul>
	 * 
	 * @param mediaKey
	 *            media key to register
	 * @param listener
	 *            listener to be notified of hotkey events
	 * @see MediaKey
	 */
	public abstract void register(MediaKey mediaKey, HotKeyListener listener);

	/**
	 * Reset all hotkey listeners
	 */
	public abstract void reset();

	public void setUseSwingEventQueue(final boolean useSwingEventQueue) {
		this.useSwingEventQueue = useSwingEventQueue;
	}

	/**
	 * Stop the provider. Stops main thread and frees any resources. </br> all
	 * hotkeys should be reset before calling this method
	 * 
	 * @see HotkeyProvider#reset()
	 */
	public void stop() {
		if (eventQueue != null) {
			eventQueue.shutdown();
		}
	}

}
