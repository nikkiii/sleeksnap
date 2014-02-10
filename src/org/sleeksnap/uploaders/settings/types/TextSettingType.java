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
package org.sleeksnap.uploaders.settings.types;

import javax.swing.JComponent;
import javax.swing.JTextField;

import org.sleeksnap.uploaders.settings.UploaderSettingType;

/**
 * The most basic setting type, a JTextField
 * 
 * @author Nikki
 * 
 */
public class TextSettingType implements UploaderSettingType {

	@Override
	public JComponent constructComponent(final String defaultValue) {
		return new JTextField(defaultValue.toString());
	}

	@Override
	public Object getValue(final JComponent component) {
		return ((JTextField) component).getText();
	}

	@Override
	public void setValue(final JComponent component, final Object value) {
		((JTextField) component).setText(value.toString());
	}

}
