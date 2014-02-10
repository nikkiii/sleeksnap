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

package com.tulskiy.keymaster.osx;

import static com.tulskiy.keymaster.osx.Carbon.Lib;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.swing.KeyStroke;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.HotkeyProvider;
import com.tulskiy.keymaster.common.MediaKey;
import com.tulskiy.keymaster.osx.Carbon.EventHandlerProcPtr;
import com.tulskiy.keymaster.osx.Carbon.EventHotKeyID;
import com.tulskiy.keymaster.osx.Carbon.EventTypeSpec;

/**
 * Author: Denis Tulskiy Date: 6/17/11
 */
public class CarbonProvider extends HotkeyProvider {
	private class EventHandler implements Carbon.EventHandlerProcPtr {
		@Override
		public int callback(final Pointer inHandlerCallRef,
				final Pointer inEvent, final Pointer inUserData) {
			final EventHotKeyID eventHotKeyID = new EventHotKeyID();
			final int ret = Lib.GetEventParameter(inEvent,
					kEventParamDirectObject, typeEventHotKeyID, null,
					eventHotKeyID.size(), null, eventHotKeyID);
			if (ret != 0) {
				logger.warning("Could not get event parameters. Error code: "
						+ ret);
			} else {
				final int eventId = eventHotKeyID.id;
				logger.info("Received event id: " + eventId);
				fireEvent(hotKeys.get(eventId));
			}
			return 0;
		}
	}

	class OSXHotKey extends HotKey {
		PointerByReference handler;

		public OSXHotKey(final KeyStroke keyStroke,
				final HotKeyListener listener) {
			super(keyStroke, listener);
		}
	}

	private static int idSeq = 1;
	private static final int kEventClassKeyboard = OS_TYPE("keyb");

	private static final int kEventHotKeyPressed = 5;

	private static final int kEventParamDirectObject = OS_TYPE("----");
	private static final int typeEventHotKeyID = OS_TYPE("hkid");

	private static int OS_TYPE(final String osType) {
		final byte[] bytes = osType.getBytes();
		return (bytes[0] << 24) + (bytes[1] << 16) + (bytes[2] << 8) + bytes[3];
	}

	private PointerByReference eventHandlerReference;
	private final Map<Integer, OSXHotKey> hotKeys = new HashMap<Integer, OSXHotKey>();

	private EventHandlerProcPtr keyListener;
	private boolean listen;
	private final Object lock = new Object();

	private final Queue<OSXHotKey> registerQueue = new LinkedList<OSXHotKey>();

	private boolean reset;

	public Thread thread;

	@Override
	public void init() {
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (lock) {
					logger.info("Installing Event Handler");
					eventHandlerReference = new PointerByReference();
					keyListener = new EventHandler();

					final EventTypeSpec[] eventTypes = (EventTypeSpec[]) (new EventTypeSpec()
							.toArray(1));
					eventTypes[0].eventClass = kEventClassKeyboard;
					eventTypes[0].eventKind = kEventHotKeyPressed;

					final int status = Lib.InstallEventHandler(
							Lib.GetEventDispatcherTarget(), keyListener, 1,
							eventTypes, null, eventHandlerReference); // fHandlerRef
					if (status != 0) {
						logger.warning("Could not register Event Handler, error code: "
								+ status);
					}

					if (eventHandlerReference.getValue() == null) {
						logger.warning("Event Handler reference is null");
					}
					listen = true;
					while (listen) {
						if (reset) {
							resetAll();
							reset = false;
							lock.notify();
						}

						while (!registerQueue.isEmpty()) {
							register(registerQueue.poll());
						}

						try {
							lock.wait();
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		});

		thread.start();
	}

	@Override
	public void register(final KeyStroke keyCode, final HotKeyListener listener) {
		synchronized (lock) {
			registerQueue.add(new OSXHotKey(keyCode, listener));
			lock.notify();
		}
	}

	@Override
	public void register(final MediaKey mediaKey, final HotKeyListener listener) {
		logger.warning("Media keys are not supported on this platform");
	}

	private void register(final OSXHotKey hotKey) {
		final KeyStroke keyCode = hotKey.getKeyStroke();
		final EventHotKeyID.ByValue hotKeyReference = new EventHotKeyID.ByValue();
		final int id = idSeq++;
		hotKeyReference.id = id;
		hotKeyReference.signature = OS_TYPE("hk" + String.format("%02d", id));
		final PointerByReference gMyHotKeyRef = new PointerByReference();

		final int status = Lib.RegisterEventHotKey(KeyMap.getKeyCode(keyCode),
				KeyMap.getModifier(keyCode), hotKeyReference,
				Lib.GetEventDispatcherTarget(), 0, gMyHotKeyRef);

		if (status != 0) {
			logger.warning("Could not register HotKey: " + keyCode
					+ ". Error code: " + status);
			return;
		}

		if (gMyHotKeyRef.getValue() == null) {
			logger.warning("HotKey returned null handler reference");
			return;
		}
		hotKey.handler = gMyHotKeyRef;
		logger.info("Registered hotkey: " + keyCode);
		hotKeys.put(id, hotKey);
	}

	@Override
	public void reset() {
		synchronized (lock) {
			reset = true;
			lock.notify();
			try {
				lock.wait();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void resetAll() {
		logger.info("Resetting hotkeys");
		for (final OSXHotKey hotKey : hotKeys.values()) {
			final int ret = Lib
					.UnregisterEventHotKey(hotKey.handler.getValue());
			if (ret != 0) {
				logger.warning("Could not unregister hotkey. Error code: "
						+ ret);
			}
		}
		hotKeys.clear();
	}

	@Override
	public void stop() {
		logger.info("Stopping now");
		try {
			synchronized (lock) {
				listen = false;
				lock.notify();
			}
			thread.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		if (eventHandlerReference.getValue() != null) {
			Lib.RemoveEventHandler(eventHandlerReference.getValue());
		}
		super.stop();
	}

}
