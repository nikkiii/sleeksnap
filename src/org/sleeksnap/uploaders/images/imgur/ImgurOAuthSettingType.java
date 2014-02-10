package org.sleeksnap.uploaders.images.imgur;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;

import org.json.JSONObject;
import org.nikkii.embedhttp.HttpServer;
import org.nikkii.embedhttp.handler.HttpRequestHandler;
import org.nikkii.embedhttp.impl.HttpRequest;
import org.nikkii.embedhttp.impl.HttpResponse;
import org.nikkii.embedhttp.impl.HttpStatus;
import org.sleeksnap.uploaders.images.ImgurUploader;
import org.sleeksnap.uploaders.settings.UploaderSettingType;
import org.sleeksnap.util.StreamUtils;
import org.sleeksnap.util.Util;

public class ImgurOAuthSettingType implements UploaderSettingType {

	private static final String AUTH_URL = "https://api.imgur.com/oauth2/authorize?client_id="
			+ ImgurUploader.CLIENT_ID + "&response_type=token";

	private JSONObject obj = null;

	@Override
	public JComponent constructComponent(final String defaultValue) {
		final JButton button = new JButton("Click to authorize");
		button.setPreferredSize(new Dimension(200, 20));
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				try {
					imgurAuthentication(button);
				} catch (final Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		button.updateUI();
		return button;
	}

	@Override
	public Object getValue(final JComponent component) {
		return obj;
	}

	protected void imgurAuthentication(final JComponent component)
			throws Exception {
		Util.openURL(new URL(AUTH_URL));

		final HttpServer server = new HttpServer(8581);
		server.addRequestHandler(new HttpRequestHandler() {
			@Override
			public HttpResponse handleRequest(final HttpRequest request) {
				if (request.getUri().equals("/imgur-auth-finish")) {
					try {
						return new HttpResponse(
								HttpStatus.OK,
								StreamUtils
										.readContents(Util
												.getResourceByName(
														"/org/sleeksnap/uploaders/images/imgur/imgurajax.html")
												.openStream()));
					} catch (final IOException e) {
						e.printStackTrace();
					}
				} else if (request.getUri().equals("/imgur-auth-ajax")) {
					processImgurToken(component, request.getPostData());
					// AJAX Response and close server
					try {
						return new HttpResponse(
								HttpStatus.OK,
								"Thank you for authenticating! You may now close this window and return to Sleeksnap.<br />Alternatively, this window will auto close in 30 seconds.<script>setTimeout(window.close, 30000);</script>");
					} finally {
						server.stop();
					}
				}
				return null;
			}
		});
		server.start();
	}

	private void processImgurToken(final JComponent component,
			final Map<String, Object> data) {
		obj = new JSONObject(data);

		new Thread(new Runnable() {
			@Override
			public void run() {
				setValue(component, obj);
				component.getParent().requestFocus();
				JOptionPane
						.showMessageDialog(
								component,
								"Thank you for authorizing "
										+ data.get("account_username")
										+ "! Your Imgur images will now upload to your account.",
								"Thank you", JOptionPane.INFORMATION_MESSAGE);
			}
		}).start();
	}

	@Override
	public void setValue(final JComponent component, final Object value) {
		if (value instanceof JSONObject) {
			final JSONObject obj = (JSONObject) value;

			((JButton) component).setText(obj.getString("account_username"));

			this.obj = obj;
		}
	}
}
