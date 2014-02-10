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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.json.JSONObject;
import org.sleeksnap.Configuration;
import org.sleeksnap.ScreenSnapper;
import org.sleeksnap.gui.OptionPanel;
import org.sleeksnap.upload.FileUpload;
import org.sleeksnap.upload.ImageUpload;
import org.sleeksnap.upload.TextUpload;
import org.sleeksnap.upload.URLUpload;
import org.sleeksnap.upload.Upload;
import org.sleeksnap.uploaders.Settings;
import org.sleeksnap.uploaders.Uploader;
import org.sleeksnap.uploaders.UploaderConfigurationException;
import org.sleeksnap.uploaders.settings.ParametersDialog;
import org.sleeksnap.uploaders.settings.UploaderSettings;
import org.sleeksnap.util.Util;
import org.sleeksnap.util.Utils.SortingUtil;

/**
 * An OptionSubPanel for the Uploader settings options
 * 
 * @author Nikki
 * 
 */
@SuppressWarnings({ "serial", "unchecked", "rawtypes" })
public class UploaderPanel extends OptionSubPanel {

	private class OpenSettingsListener implements ActionListener {
		private final JComboBox comboBox;

		public OpenSettingsListener(final JComboBox comboBox) {
			this.comboBox = comboBox;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final Object selected = comboBox.getSelectedItem();
			if (selected instanceof UploaderWrapper) {
				openSettings(((UploaderWrapper) selected).getUploader());
			}
		}
	}

	private class SettingsListener implements ActionListener {

		private final JComboBox comboBox;
		private final JButton settingsButton;

		public SettingsListener(final JComboBox comboBox,
				final JButton settingsButton) {
			this.comboBox = comboBox;
			this.settingsButton = settingsButton;
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			final Object selected = comboBox.getSelectedItem();
			if (selected instanceof UploaderWrapper) {
				settingsButton.setEnabled(((UploaderWrapper) selected)
						.hasSettings());
			}
		}
	}

	private class UploaderWrapper {
		private final boolean hasSettings;
		private final Uploader<?> uploader;

		public UploaderWrapper(final Uploader<?> uploader) {
			this.uploader = uploader;
			hasSettings = uploader.hasSettings();
		}

		public Uploader<?> getUploader() {
			return uploader;
		}

		public boolean hasSettings() {
			return hasSettings;
		}

		@Override
		public String toString() {
			return uploader.getName();
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8928404286491601686L;
	private JCheckBox automaticUpload;
	private JButton browseButton;

	private JLabel fileLabel;
	private DefaultComboBoxModel fileModel;

	private JButton fileSettings;
	private JComboBox fileUploader;
	private JLabel imageLabel;
	private DefaultComboBoxModel imageModel;

	private JButton imageSettings;
	private JComboBox imageUploader;
	private JCheckBox localCopyCheckbox;
	private final OptionPanel parent;

	private JButton saveButton;
	private JCheckBox shortenURLs;
	private JLabel textLabel;

	private DefaultComboBoxModel textModel;
	private JButton textSettings;
	private JComboBox textUploader;
	private JLabel urlLabel;

	private DefaultComboBoxModel urlModel;

	private JButton urlSettings;

	private JComboBox urlShortener;

	public UploaderPanel(final OptionPanel parent) {
		this.parent = parent;
	}

	private void browseButtonActionPerformed(
			final java.awt.event.ActionEvent evt) {
		try {
			Desktop.getDesktop().open(
					new File(Util.getWorkingDirectory(), "uploaders"));
		} catch (final IOException e1) {
			e1.printStackTrace();
		}
	}

	public Uploader<?> getActualUploader(final Uploader<?> uploader) {
		if (uploader.hasDirectSettings()) {
			return uploader;
		} else if (uploader.hasParent()
				&& uploader.getParentUploader().hasSettings()) {
			return uploader.getParentUploader();
		}
		return null;
	}

	@Override
	public void initComponents() {

		imageSettings = new JButton();
		textSettings = new JButton();
		fileSettings = new JButton();
		urlSettings = new JButton();

		imageLabel = new JLabel();
		textLabel = new JLabel();
		fileLabel = new JLabel();
		urlLabel = new JLabel();

		imageUploader = new JComboBox();
		textUploader = new JComboBox();
		fileUploader = new JComboBox();
		saveButton = new JButton();
		browseButton = new JButton();
		automaticUpload = new JCheckBox();
		shortenURLs = new JCheckBox();
		urlShortener = new JComboBox();
		localCopyCheckbox = new JCheckBox();

		imageModel = new DefaultComboBoxModel();
		textModel = new DefaultComboBoxModel();
		fileModel = new DefaultComboBoxModel();
		urlModel = new DefaultComboBoxModel();

		final ActionListener changeListener = new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (!saveButton.isEnabled()) {
					saveButton.setEnabled(true);
				}
			}
		};

		imageUploader.addActionListener(changeListener);
		textUploader.addActionListener(changeListener);
		fileUploader.addActionListener(changeListener);
		urlShortener.addActionListener(changeListener);

		shortenURLs.addActionListener(changeListener);
		automaticUpload.addActionListener(changeListener);
		localCopyCheckbox.addActionListener(changeListener);

		imageLabel.setText("Images");

		imageUploader.setModel(imageModel);

		imageUploader.addActionListener(new SettingsListener(imageUploader,
				imageSettings));

		textLabel.setText("Text");

		textUploader.setModel(textModel);

		textUploader.addActionListener(new SettingsListener(textUploader,
				textSettings));

		fileLabel.setText("Files");

		fileUploader.setModel(fileModel);

		fileUploader.addActionListener(new SettingsListener(fileUploader,
				fileSettings));

		urlLabel.setText("URLs");

		urlShortener.setModel(urlModel);

		urlShortener.addActionListener(new SettingsListener(urlShortener,
				urlSettings));

		saveButton.setText("Save");
		saveButton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt) {
				savePreferences();
			}
		});

		browseButton.setText("Browse Directory");
		browseButton.addActionListener(new java.awt.event.ActionListener() {
			@Override
			public void actionPerformed(final java.awt.event.ActionEvent evt) {
				browseButtonActionPerformed(evt);
			}
		});

		// Temporary
		final Configuration configuration = parent.getConfiguration();

		automaticUpload.setText("Upload text files to text uploader");
		if (configuration.contains("plainTextUpload")) {
			automaticUpload.setSelected(configuration
					.getBoolean("plainTextUpload"));
		}

		shortenURLs.setText("Automatically shorten URLs");
		if (configuration.contains("shortenurls")) {
			shortenURLs.setSelected(configuration.getBoolean("shortenurls"));
		}

		localCopyCheckbox.setText("Keep a local copy of uploaded images");
		if (configuration.contains("savelocal")) {
			localCopyCheckbox
					.setSelected(configuration.getBoolean("savelocal"));
		}

		imageSettings.setText("Settings");
		imageSettings.setMaximumSize(new java.awt.Dimension(71, 20));
		imageSettings.setMinimumSize(new java.awt.Dimension(71, 20));
		imageSettings.setPreferredSize(new java.awt.Dimension(71, 20));

		textSettings.setText("Settings");
		textSettings.setMaximumSize(new java.awt.Dimension(71, 20));
		textSettings.setMinimumSize(new java.awt.Dimension(71, 20));
		textSettings.setPreferredSize(new java.awt.Dimension(90, 20));

		fileSettings.setText("Settings");
		fileSettings.setMaximumSize(new java.awt.Dimension(71, 20));
		fileSettings.setMinimumSize(new java.awt.Dimension(71, 20));
		fileSettings.setPreferredSize(new java.awt.Dimension(90, 20));

		urlSettings.setText("Settings");
		urlSettings.setMaximumSize(new java.awt.Dimension(71, 20));
		urlSettings.setMinimumSize(new java.awt.Dimension(71, 20));
		urlSettings.setPreferredSize(new java.awt.Dimension(90, 20));

		imageSettings.setText("Settings");
		imageSettings.setMaximumSize(new java.awt.Dimension(71, 20));
		imageSettings.setMinimumSize(new java.awt.Dimension(71, 20));
		imageSettings.setPreferredSize(new java.awt.Dimension(71, 20));

		imageSettings
				.addActionListener(new OpenSettingsListener(imageUploader));

		textSettings.addActionListener(new OpenSettingsListener(textUploader));

		fileSettings.addActionListener(new OpenSettingsListener(fileUploader));

		urlSettings.addActionListener(new OpenSettingsListener(urlShortener));

		final javax.swing.GroupLayout uploaderPanelLayout = new javax.swing.GroupLayout(
				this);
		setLayout(uploaderPanelLayout);
		uploaderPanelLayout
				.setHorizontalGroup(uploaderPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								uploaderPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addGroup(
												uploaderPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.LEADING)
														.addGroup(
																uploaderPanelLayout
																		.createSequentialGroup()
																		.addGroup(
																				uploaderPanelLayout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.LEADING)
																						.addComponent(
																								localCopyCheckbox)
																						.addComponent(
																								urlLabel)
																						.addComponent(
																								shortenURLs)
																						.addComponent(
																								automaticUpload))
																		.addGap(152,
																				152,
																				152))
														.addGroup(
																uploaderPanelLayout
																		.createSequentialGroup()
																		.addComponent(
																				browseButton)
																		.addPreferredGap(
																				javax.swing.LayoutStyle.ComponentPlacement.RELATED,
																				242,
																				Short.MAX_VALUE)
																		.addComponent(
																				saveButton,
																				javax.swing.GroupLayout.PREFERRED_SIZE,
																				70,
																				javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addContainerGap())
														.addGroup(
																uploaderPanelLayout
																		.createSequentialGroup()
																		.addGroup(
																				uploaderPanelLayout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.LEADING)
																						.addGroup(
																								uploaderPanelLayout
																										.createSequentialGroup()
																										.addGroup(
																												uploaderPanelLayout
																														.createParallelGroup(
																																javax.swing.GroupLayout.Alignment.TRAILING)
																														.addComponent(
																																imageLabel,
																																javax.swing.GroupLayout.Alignment.LEADING)
																														.addComponent(
																																textLabel,
																																javax.swing.GroupLayout.Alignment.LEADING)
																														.addComponent(
																																fileLabel,
																																javax.swing.GroupLayout.Alignment.LEADING))
																										.addGap(0,
																												0,
																												Short.MAX_VALUE))
																						.addComponent(
																								imageUploader,
																								javax.swing.GroupLayout.Alignment.TRAILING,
																								0,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE)
																						.addComponent(
																								textUploader,
																								0,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE)
																						.addComponent(
																								fileUploader,
																								0,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE)
																						.addComponent(
																								urlShortener,
																								javax.swing.GroupLayout.Alignment.TRAILING,
																								0,
																								javax.swing.GroupLayout.DEFAULT_SIZE,
																								Short.MAX_VALUE))
																		.addGap(18,
																				18,
																				18)
																		.addGroup(
																				uploaderPanelLayout
																						.createParallelGroup(
																								javax.swing.GroupLayout.Alignment.LEADING)
																						.addGroup(
																								uploaderPanelLayout
																										.createParallelGroup(
																												javax.swing.GroupLayout.Alignment.LEADING)
																										.addComponent(
																												fileSettings,
																												javax.swing.GroupLayout.Alignment.TRAILING,
																												javax.swing.GroupLayout.PREFERRED_SIZE,
																												javax.swing.GroupLayout.DEFAULT_SIZE,
																												javax.swing.GroupLayout.PREFERRED_SIZE)
																										.addComponent(
																												urlSettings,
																												javax.swing.GroupLayout.Alignment.TRAILING,
																												javax.swing.GroupLayout.PREFERRED_SIZE,
																												javax.swing.GroupLayout.DEFAULT_SIZE,
																												javax.swing.GroupLayout.PREFERRED_SIZE)
																										.addComponent(
																												textSettings,
																												javax.swing.GroupLayout.Alignment.TRAILING,
																												javax.swing.GroupLayout.PREFERRED_SIZE,
																												javax.swing.GroupLayout.DEFAULT_SIZE,
																												javax.swing.GroupLayout.PREFERRED_SIZE))
																						.addGroup(
																								javax.swing.GroupLayout.Alignment.TRAILING,
																								uploaderPanelLayout
																										.createSequentialGroup()
																										.addComponent(
																												imageSettings,
																												javax.swing.GroupLayout.PREFERRED_SIZE,
																												88,
																												javax.swing.GroupLayout.PREFERRED_SIZE)
																										.addContainerGap()))))));
		uploaderPanelLayout
				.setVerticalGroup(uploaderPanelLayout
						.createParallelGroup(
								javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(
								uploaderPanelLayout
										.createSequentialGroup()
										.addContainerGap()
										.addComponent(imageLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												uploaderPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																imageUploader,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																imageSettings,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addGap(11, 11, 11)
										.addComponent(textLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												uploaderPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																textUploader,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																textSettings,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(fileLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												uploaderPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																fileUploader,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																fileSettings,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(urlLabel)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(
												uploaderPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																urlShortener,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE)
														.addComponent(
																urlSettings,
																javax.swing.GroupLayout.PREFERRED_SIZE,
																javax.swing.GroupLayout.DEFAULT_SIZE,
																javax.swing.GroupLayout.PREFERRED_SIZE))
										.addGap(18, 18, 18)
										.addComponent(shortenURLs)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(automaticUpload)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(localCopyCheckbox)
										.addPreferredGap(
												javax.swing.LayoutStyle.ComponentPlacement.RELATED,
												140, Short.MAX_VALUE)
										.addGroup(
												uploaderPanelLayout
														.createParallelGroup(
																javax.swing.GroupLayout.Alignment.BASELINE)
														.addComponent(
																saveButton)
														.addComponent(
																browseButton))
										.addContainerGap()));
	}

	public void openSettings(final Uploader<?> uploader) {
		final Uploader<?> actualUploader = getActualUploader(uploader);

		if (actualUploader == null) {
			return;
		}

		final Settings settings = actualUploader.getSettingsAnnotation();

		JFrame frame = null;

		if (getParent().getParent() instanceof JFrame) {
			frame = (JFrame) getParent().getParent();
		}

		final ParametersDialog dialog = new ParametersDialog(frame,
				actualUploader, settings);
		dialog.setModal(true);
		dialog.setOkAction(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				final UploaderSettings newSettings = dialog
						.toSettings(actualUploader.getSettings());
				try {
					if (actualUploader.validateSettings(newSettings)) {
						// Close the window
						dialog.closeWindow();
						// Set the uploader's settings
						actualUploader.setSettings(newSettings);
						// Finally, save the settings
						try {
							actualUploader.saveSettings(ScreenSnapper
									.getSettingsFile(actualUploader.getClass()));
						} catch (final Exception ex) {
							JOptionPane.showMessageDialog(null,
									"Save failed! Caused by: " + ex,
									"Save failed", JOptionPane.ERROR_MESSAGE);
						}
					}
				} catch (final UploaderConfigurationException e1) {
					JOptionPane.showMessageDialog(dialog,
							"Uploader settings not saved due to error\nCause:\n"
									+ e1.getMessage(), "Error saving settings",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		dialog.setVisible(true);
	}

	public void savePreferences() {
		final ScreenSnapper snapper = parent.getSnapper();

		if (imageUploader.getSelectedItem() != null) {
			final Uploader<?> imageSelection = ((UploaderWrapper) imageUploader
					.getSelectedItem()).getUploader();
			if (imageSelection != snapper.getUploaderFor(ImageUpload.class)) {
				snapper.setDefaultUploader(imageSelection, true);
			}
		}
		if (textUploader.getSelectedItem() != null) {
			final Uploader<?> textSelection = ((UploaderWrapper) textUploader
					.getSelectedItem()).getUploader();
			if (textSelection != snapper.getUploaderFor(TextUpload.class)) {
				snapper.setDefaultUploader(textSelection, true);
			}
		}
		if (fileUploader.getSelectedItem() != null) {
			final Uploader<?> fileSelection = ((UploaderWrapper) fileUploader
					.getSelectedItem()).getUploader();
			if (fileSelection != snapper.getUploaderFor(FileUpload.class)) {
				snapper.setDefaultUploader(fileSelection, true);
			}
		}
		if (urlShortener.getSelectedItem() != null) {
			final Uploader<?> urlSelection = ((UploaderWrapper) urlShortener
					.getSelectedItem()).getUploader();
			if (urlSelection != snapper.getUploaderFor(URLUpload.class)) {
				snapper.setDefaultUploader(urlSelection, true);
			}
		}
		final JSONObject uploaders = new JSONObject();
		for (final Entry<Class<? extends Upload>, Uploader<?>> entry : snapper
				.getUploaderAssociations().entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			uploaders.put(entry.getKey().getName(), entry.getValue().getClass()
					.getName());
		}
		final Configuration config = parent.getConfiguration();
		config.put("uploaders", uploaders);
		config.put("shortenurls", shortenURLs.isSelected());
		config.put("plainTextUpload", automaticUpload.isSelected());
		config.put("savelocal", localCopyCheckbox.isSelected());
		try {
			config.save();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		saveButton.setEnabled(false);
	}

	public void setFileUploaders(final Collection<Uploader<?>> uploaders) {
		final Uploader<?> basic = parent.getSnapper().getUploaderFor(
				FileUpload.class);
		for (final Uploader<?> uploader : SortingUtil.sortUploaders(uploaders)) {
			final UploaderWrapper wrapper = new UploaderWrapper(uploader);
			fileModel.addElement(wrapper);
			if (basic == uploader) {
				fileModel.setSelectedItem(wrapper);
				fileSettings.setEnabled(wrapper.hasSettings());
			}
		}
	}

	public void setImageUploaders(final Collection<Uploader<?>> uploaders) {
		final Uploader<?> basic = parent.getSnapper().getUploaderFor(
				ImageUpload.class);
		for (final Uploader<?> uploader : SortingUtil.sortUploaders(uploaders)) {
			final UploaderWrapper wrapper = new UploaderWrapper(uploader);
			imageModel.addElement(wrapper);
			if (basic == uploader) {
				imageModel.setSelectedItem(wrapper);
				imageSettings.setEnabled(wrapper.hasSettings());
			}
		}
	}

	public void setTextUploaders(final Collection<Uploader<?>> uploaders) {
		final Uploader<?> basic = parent.getSnapper().getUploaderFor(
				TextUpload.class);
		for (final Uploader<?> uploader : SortingUtil.sortUploaders(uploaders)) {
			final UploaderWrapper wrapper = new UploaderWrapper(uploader);
			textModel.addElement(wrapper);
			if (basic == uploader) {
				textModel.setSelectedItem(wrapper);
				textSettings.setEnabled(wrapper.hasSettings());
			}
		}
	}

	public void setURLUploaders(final Collection<Uploader<?>> uploaders) {
		final Uploader<?> basic = parent.getSnapper().getUploaderFor(
				URLUpload.class);
		for (final Uploader<?> uploader : SortingUtil.sortUploaders(uploaders)) {
			final UploaderWrapper wrapper = new UploaderWrapper(uploader);
			urlModel.addElement(wrapper);
			if (basic == uploader) {
				urlModel.setSelectedItem(wrapper);
				urlSettings.setEnabled(wrapper.hasSettings());
			}
		}
	}
}
