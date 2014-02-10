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

package com.tulskiy.keymaster.x11;

import static com.tulskiy.keymaster.x11.X11.ControlMask;
import static com.tulskiy.keymaster.x11.X11.GrabModeAsync;
import static com.tulskiy.keymaster.x11.X11.KeyPress;
import static com.tulskiy.keymaster.x11.X11.Lib;
import static com.tulskiy.keymaster.x11.X11.LockMask;
import static com.tulskiy.keymaster.x11.X11.Mod1Mask;
import static com.tulskiy.keymaster.x11.X11.Mod2Mask;
import static com.tulskiy.keymaster.x11.X11.Mod3Mask;
import static com.tulskiy.keymaster.x11.X11.Mod4Mask;
import static com.tulskiy.keymaster.x11.X11.Mod5Mask;
import static com.tulskiy.keymaster.x11.X11.ShiftMask;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.swing.KeyStroke;

import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.tulskiy.keymaster.common.HotKey;
import com.tulskiy.keymaster.common.HotKeyListener;
import com.tulskiy.keymaster.common.HotkeyProvider;
import com.tulskiy.keymaster.common.MediaKey;
import com.tulskiy.keymaster.x11.X11.XErrorEvent;
import com.tulskiy.keymaster.x11.X11.XErrorHandler;
import com.tulskiy.keymaster.x11.X11.XEvent;
import com.tulskiy.keymaster.x11.X11.XKeyEvent;

/**
 * Author: Denis Tulskiy Date: 6/13/11
 */
public class X11Provider extends HotkeyProvider {
	class ErrorHandler implements XErrorHandler {
		@Override
		public int apply(final Pointer display, final XErrorEvent errorEvent) {
			final byte[] buf = new byte[1024];
			Lib.XGetErrorText(display, errorEvent.error_code, buf, buf.length);
			int len = 0;
			while (buf[len] != 0) {
				len++;
			}
			logger.warning("Error: " + new String(buf, 0, len));
			return 0;
		}
	}

	class X11HotKey extends HotKey {
		byte code;
		int modifiers;

		X11HotKey(final KeyStroke keyStroke, final HotKeyListener listener) {
			super(keyStroke, listener);
		}

		X11HotKey(final MediaKey mediaKey, final HotKeyListener listener) {
			super(mediaKey, listener);
		}
	}

	private Pointer display;
	private ErrorHandler errorHandler;
	private final List<X11HotKey> hotKeys = new ArrayList<X11HotKey>();
	private boolean listening;
	private final Object lock = new Object();
	private final Queue<X11HotKey> registerQueue = new LinkedList<X11HotKey>();
	private boolean reset;

	private Thread thread;

	private NativeLong window;

	public X11Provider() {
		// Call XInitThreads FIRST
		Lib.XInitThreads();
	}

	private int correctModifiers(final int modifiers, final int flags) {
		int ret = modifiers;
		if ((flags & 1) != 0) {
			ret |= LockMask;
		}
		if ((flags & 2) != 0) {
			ret |= Mod2Mask;
		}
		if ((flags & 4) != 0) {
			ret |= Mod3Mask;
		}
		if ((flags & 8) != 0) {
			ret |= Mod5Mask;
		}
		return ret;
	}

	@Override
	public void init() {
		final Runnable runnable = new Runnable() {
			@Override
			public void run() {
				logger.info("Starting X11 global hotkey provider");
				display = Lib.XOpenDisplay(null);
				errorHandler = new ErrorHandler();
				Lib.XSetErrorHandler(errorHandler);
				window = Lib.XDefaultRootWindow(display);
				listening = true;
				final XEvent event = new XEvent();

				while (listening) {
					while (Lib.XPending(display) > 0) {
						Lib.XNextEvent(display, event);
						if (event.type == KeyPress) {
							final XKeyEvent xkey = (XKeyEvent) event
									.readField("xkey");
							for (final X11HotKey hotKey : hotKeys) {
								final int state = xkey.state
										& (ShiftMask | ControlMask | Mod1Mask | Mod4Mask);
								if (hotKey.code == (byte) xkey.keycode
										&& hotKey.modifiers == state) {
									logger.info("Received event for hotkey: "
											+ hotKey);
									fireEvent(hotKey);
									break;
								}
							}
						}
					}

					synchronized (lock) {
						if (reset) {
							logger.info("Reset hotkeys");
							resetAll();
							reset = false;
							lock.notify();
						}

						while (!registerQueue.isEmpty()) {
							final X11HotKey hotKey = registerQueue.poll();
							logger.info("Registering hotkey: " + hotKey);
							if (hotKey.isMedia()) {
								registerMedia(hotKey);
							} else {
								register(hotKey);
							}
							hotKeys.add(hotKey);
						}

						try {
							lock.wait(300);
						} catch (final InterruptedException e) {
							e.printStackTrace();
						}
					}

				}

				logger.info("Thread - stop listening");
			}
		};

		thread = new Thread(runnable);
		thread.start();
	}

	@Override
	public void register(final KeyStroke keyCode, final HotKeyListener listener) {
		synchronized (lock) {
			registerQueue.add(new X11HotKey(keyCode, listener));
		}
	}

	@Override
	public void register(final MediaKey mediaKey, final HotKeyListener listener) {
		synchronized (lock) {
			registerQueue.add(new X11HotKey(mediaKey, listener));
		}
	}

	private void register(final X11HotKey hotKey) {
		final byte code = KeyMap.getCode(hotKey.getKeyStroke(), display);
		if (code == 0) {
			return;
		}
		final int modifiers = KeyMap.getModifiers(hotKey.getKeyStroke());
		hotKey.code = code;
		hotKey.modifiers = modifiers;
		for (int i = 0; i < 16; i++) {
			final int flags = correctModifiers(modifiers, i);

			Lib.XGrabKey(display, code, flags, window, 1, GrabModeAsync,
					GrabModeAsync);
		}
	}

	private void registerMedia(final X11HotKey hotKey) {
		final byte keyCode = KeyMap.getMediaCode(hotKey.getMediaKey(), display);
		hotKey.modifiers = 0;
		hotKey.code = keyCode;
		Lib.XGrabKey(display, keyCode, 0, window, 1, GrabModeAsync,
				GrabModeAsync);
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

	private void resetAll() {
		for (final X11HotKey hotKey : hotKeys) {
			if (!hotKey.isMedia()) {
				final int modifiers = hotKey.modifiers;
				for (int i = 0; i < 16; i++) {
					final int flags = correctModifiers(modifiers, i);

					Lib.XUngrabKey(display, hotKey.code, flags, window);
				}
			} else {
				Lib.XUngrabKey(display, hotKey.code, 0, window);
			}
		}

		hotKeys.clear();
	}

	@Override
	public void stop() {
		if (thread != null) {
			listening = false;
			try {
				thread.join();
				Lib.XCloseDisplay(display);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
		super.stop();
	}
}
