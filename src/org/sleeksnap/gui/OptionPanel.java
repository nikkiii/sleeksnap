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
package org.sleeksnap.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;

import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.sleeksnap.Configuration;
import org.sleeksnap.ScreenSnapper;
import org.sleeksnap.gui.options.HistoryPanel;
import org.sleeksnap.gui.options.HotkeyPanel;
import org.sleeksnap.gui.options.InfoPanel;
import org.sleeksnap.gui.options.LogPanel;
import org.sleeksnap.gui.options.OptionSubPanel;
import org.sleeksnap.gui.options.UpdaterPanel;
import org.sleeksnap.gui.options.UploaderPanel;
import org.sleeksnap.impl.History;
import org.sleeksnap.util.StreamUtils;
import org.sleeksnap.util.Util;

/**
 * 
 * @author Nikki
 */
@SuppressWarnings({ "serial" })
public class OptionPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5423078207600912960L;

	private HistoryPanel historyPanel;

	private HotkeyPanel hotkeyPanel;

	private InfoPanel infoPanel;

	private JTabbedPane jTabbedPane1;

	private LogPanel logPanel;

	private int previousTab = 0;

	private final ScreenSnapper snapper;

	private UploaderPanel uploaderPanel;

	public OptionPanel(final ScreenSnapper snapper) {
		this.snapper = snapper;
		initComponents();
	}

	public void doneBuilding() {
		infoPanel.doneBuilding();
		uploaderPanel.doneBuilding();
		hotkeyPanel.doneBuilding();
		historyPanel.doneBuilding();
		logPanel.doneBuilding();
	}

	public Configuration getConfiguration() {
		return snapper.getConfiguration();
	}

	public ScreenSnapper getSnapper() {
		return snapper;
	}

	public UploaderPanel getUploaderPanel() {
		return uploaderPanel;
	}

	private void initComponents() {

		jTabbedPane1 = new JTabbedPane();

		// New panels
		infoPanel = new InfoPanel(this);
		infoPanel.initComponents();

		uploaderPanel = new UploaderPanel(this);
		uploaderPanel.initComponents();

		hotkeyPanel = new HotkeyPanel(this);
		hotkeyPanel.initComponents();

		historyPanel = new HistoryPanel(this);
		historyPanel.initComponents();

		logPanel = new LogPanel(this);
		logPanel.initComponents();

		setMinimumSize(new java.awt.Dimension(500, 300));

		jTabbedPane1.setTabPlacement(SwingConstants.LEFT);
		jTabbedPane1.setCursor(new java.awt.Cursor(
				java.awt.Cursor.DEFAULT_CURSOR));

		jTabbedPane1.addTab("Main", infoPanel);

		jTabbedPane1.addTab("Uploaders", uploaderPanel);

		jTabbedPane1.addTab("Hotkeys", hotkeyPanel);

		jTabbedPane1.addTab("History", historyPanel);

		jTabbedPane1.addTab("Log", logPanel);

		initializeTab("Updater", UpdaterPanel.class);

		final GroupLayout layout = new GroupLayout(this);
		setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(
				GroupLayout.Alignment.LEADING).addComponent(jTabbedPane1,
				GroupLayout.PREFERRED_SIZE, 520, GroupLayout.PREFERRED_SIZE));
		layout.setVerticalGroup(layout.createParallelGroup(
				GroupLayout.Alignment.LEADING).addComponent(jTabbedPane1,
				GroupLayout.PREFERRED_SIZE, 470, GroupLayout.PREFERRED_SIZE));

		jTabbedPane1.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				final int index = jTabbedPane1.getSelectedIndex();
				if (index == 4) {
					// Load the log
					final File file = new File(Util.getWorkingDirectory(),
							"log.txt");
					try {
						final String contents = StreamUtils
								.readContents(new FileInputStream(file));
						logPanel.setContents(contents);
					} catch (final IOException ex) {
						ex.printStackTrace();
					}
				}
				if (previousTab == 2) {
					// Restart our input manager if we disabled it
					if (!snapper.getKeyManager().hasKeysBound()) {
						snapper.getKeyManager().initializeInput();
					}
				}
				previousTab = index;
			}
		});

		// Do any loading/initializing
		hotkeyPanel.loadCurrentHotkeys();
	}

	public void initializeTab(final String name, final Class<?> cl) {
		try {
			final Constructor<?> c = cl.getConstructor(OptionPanel.class);
			if (c == null) {
				throw new NoSuchMethodException(
						"Unable to find a valid constructor!");
			}

			final OptionSubPanel panel = (OptionSubPanel) c.newInstance(this);

			panel.initComponents();

			jTabbedPane1.addTab(name, panel);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public void saveAll() {
		hotkeyPanel.savePreferences();
		uploaderPanel.savePreferences();
	}

	public void setHistory(final History history) {
		historyPanel.setHistory(history);
	}
}
