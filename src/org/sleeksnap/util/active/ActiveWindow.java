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
package org.sleeksnap.util.active;

import java.awt.Rectangle;

/**
 * Represents an Active window
 * 
 * @author Nikki
 * 
 */
public class ActiveWindow {

	/**
	 * The window's bounds
	 */
	private final Rectangle bounds;

	/**
	 * The window name
	 */
	private final String name;

	public ActiveWindow(final String name, final Rectangle bounds) {
		this.name = name;
		this.bounds = bounds;
	}

	/**
	 * Get the window's bounds as a rectangle
	 * 
	 * @return A rectangle containing the window's bounds
	 */
	public Rectangle getBounds() {
		return bounds;
	}

	/**
	 * Get the window name
	 * 
	 * @return The window name
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name + " [x=" + bounds.x + ",y=" + bounds.y + ",width="
				+ bounds.width + ",height=" + bounds.height + "]";
	}
}