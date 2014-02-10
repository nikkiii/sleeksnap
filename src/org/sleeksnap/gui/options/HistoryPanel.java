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
package org.sleeksnap.gui.options;

import java.awt.Desktop;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.sleeksnap.gui.OptionPanel;
import org.sleeksnap.impl.History;
import org.sleeksnap.impl.HistoryEntry;
import org.sleeksnap.util.Utils.ClipboardUtil;

/**
 * An OptionSubPanel for History elements
 * 
 * @author Nikki
 * 
 */
@SuppressWarnings({ "serial", "unchecked", "rawtypes", "unused" })
public class HistoryPanel extends OptionSubPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3126147885880571093L;

	private JButton historyCopy;

	private JList historyList;
	private JButton historyOpen;
	private JScrollPane historyScroll;

	private JButton historySelect;

	private JTextField linkField;
	private JLabel linkLabel;

	public HistoryPanel(final OptionPanel parent) {

	}

	private void historyCopyActionPerformed(final java.awt.event.ActionEvent evt) {
		final String text = linkField.getText();
		if (!text.equals("")) {
			ClipboardUtil.setClipboard(text);
		}
	}

	private void historyOpenActionPerformed(final java.awt.event.ActionEvent evt) {
		final String text = linkField.getText();
		if (!text.equals("")) {
			try {
				Desktop.getDesktop().browse(new URL(text).toURI());
			} catch (final MalformedURLException e) {
				e.printStackTrace();
			} catch (final IOException e) {
				e.printStackTrace();
			} catch (final URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}

	private void historySelectActionPerformed(
			final java.awt.event.ActionEvent evt) {
		final String text = linkField.getText();
		if (!text.equals("")) {
			linkField.select(0, text.length());
		}
	}

	@Override
	public void initComponents() {
		historyScroll = new JScrollPane();
		historyList = new JList();
		linkLabel = new JLabel();
		linkField = new JTextField();
		historyOpen = new JButton();
		historyCopy = new JButton();
		historySelect = new JButton();

		historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		historyList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					final Object value = historyList.getSelectedValue();
					if (value != null) {
						final HistoryEntry entry = (HistoryEntry) value;
						linkField.setText(entry.getUrl());
					}
				}
			}
		});

		historyScroll.setViewportView(historyList);

		linkLabel.setText("Link:");

		linkField.setEditable(false);

		historyOpen.setText("Open");
		historyOpen.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt) {
				historyOpenActionPerformed(evt);
			}
		});

		historyCopy.setText("Copy");
		historyCopy.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt) {
				historyCopyActionPerformed(evt);
			}
		});

		historySelect.setText("Select");
		historySelect.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt) {
				historySelectActionPerformed(evt);
			}
		});

		final GroupLayout historyPanelLayout = new GroupLayout(this);
		setLayout(historyPanelLayout);
		historyPanelLayout
				.setHorizontalGroup(historyPanelLayout
						.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(
								historyPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												historyPanelLayout
														.createParallelGroup(
																GroupLayout.Alignment.LEADING)
														.addComponent(
																historyScroll,
																GroupLayout.DEFAULT_SIZE,
																411,
																Short.MAX_VALUE)
														.addGroup(
																historyPanelLayout
																		.createParallelGroup(
																				GroupLayout.Alignment.LEADING,
																				false)
																		.addGroup(
																				historyPanelLayout
																						.createSequentialGroup()
																						.addComponent(
																								historyOpen)
																						.addPreferredGap(
																								LayoutStyle.ComponentPlacement.RELATED,
																								GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE)
																						.addComponent(
																								historySelect)
																						.addPreferredGap(
																								LayoutStyle.ComponentPlacement.RELATED)
																						.addComponent(
																								historyCopy))
																		.addComponent(
																				linkField,
																				GroupLayout.PREFERRED_SIZE,
																				188,
																				GroupLayout.PREFERRED_SIZE)
																		.addComponent(
																				linkLabel)))
										.addContainerGap()));
		historyPanelLayout
				.setVerticalGroup(historyPanelLayout
						.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addGroup(
								historyPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(linkLabel)
										.addPreferredGap(
												LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(linkField,
												GroupLayout.PREFERRED_SIZE,
												GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(
												LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												historyPanelLayout
														.createParallelGroup(
																GroupLayout.Alignment.BASELINE)
														.addComponent(
																historyOpen)
														.addComponent(
																historyCopy)
														.addComponent(
																historySelect))
										.addPreferredGap(
												LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(historyScroll,
												GroupLayout.DEFAULT_SIZE, 363,
												Short.MAX_VALUE)
										.addContainerGap()));
	}

	public void setHistory(final History history) {
		final DefaultListModel model = new DefaultListModel();
		final List<HistoryEntry> list = history.getHistory();
		for (int i = list.size() - 1; i >= 0; i--) {
			model.addElement(list.get(i));
		}
		historyList.setModel(model);
	}
}
