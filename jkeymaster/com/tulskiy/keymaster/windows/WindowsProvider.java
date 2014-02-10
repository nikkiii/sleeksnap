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

package com.tulskiy.keymaster.windows;

import static com.tulskiy.keymaster.windows.User32.PM_REMOVE;
import static com.tulskiy.keymaster.windows.User32.PeekMessage;
import static com.tulskiy.keymaster.windows.User32.RegisterHotKey;
import static com.tulskiy.keymaster.windows.User32.UnregisterHotKey;
import static com.tulskiy.keymaster.windows.User32.WM_HOTKEY;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.swing.KeyStroke;

import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.HotkeyProvider;
import com.tulskiy.keymaster.common.MediaKey;
import com.tulskiy.keymaster.windows.User32.MSG;

/**
 * Author: Denis Tulskiy Date: 6/12/11
 */
public class WindowsProvider extends HotkeyProvider {
	private static volatile int idSeq = 0;

	private final Map<Integer, HotKey> hotKeys = new HashMap<Integer, HotKey>();
	private boolean listen;
	private final Object lock = new Object();
	private final Queue<HotKey> registerQueue = new LinkedList<HotKey>();

	private Boolean reset = false;
	private Thread thread;

	@Override
	public void init() {
		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				logger.info("Starting Windows global hotkey provider");
				final MSG msg = new MSG();
				listen = true;
				while (listen) {
					while (PeekMessage(msg, null, 0, 0, PM_REMOVE)) {
						if (msg.message == WM_HOTKEY) {
							final int id = msg.wParam.intValue();
							final HotKey hotKey = hotKeys.get(id);

							if (hotKey != null) {
								fireEvent(hotKey);
							}
						}
					}

					synchronized (lock) {
						if (reset) {
							logger.info("Reset hotkeys");
							for (final Integer id : hotKeys.keySet()) {
								UnregisterHotKey(null, id);
							}

							hotKeys.clear();
							reset = false;
							lock.notify();
						}

						while (!registerQueue.isEmpty()) {
							register(registerQueue.poll());
						}
						try {
							lock.wait(300);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				logger.info("Exit listening thread");
			}
		};

		thread = new Thread(runnable);
		thread.start();
	}

	private void register(final HotKey hotKey) {
		final int id = idSeq++;
		final int code = KeyMap.getCode(hotKey);
		if (RegisterHotKey(null, id,
				KeyMap.getModifiers(hotKey.getKeyStroke()), code)) {
			logger.info("Registering hotkey: " + hotKey);
			hotKeys.put(id, hotKey);
		} else {
			logger.warning("Could not register hotkey: " + hotKey);
		}
	}

	@Override
	public void register(final KeyStroke keyCode, final HotKeyListener listener) {
		synchronized (lock) {
			registerQueue.add(new HotKey(keyCode, listener));
		}
	}

	@Override
	public void register(final MediaKey mediaKey, final HotKeyListener listener) {
		synchronized (lock) {
			registerQueue.add(new HotKey(mediaKey, listener));
		}
	}

	@Override
	public void reset() {
		synchronized (lock) {
			reset = true;
			try {
				lock.wait();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		listen = false;
		if (thread != null) {
			try {
				thread.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		super.stop();
	}
}
