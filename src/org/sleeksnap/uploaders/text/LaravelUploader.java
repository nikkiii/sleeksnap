package org.sleeksnap.uploaders.text;

import org.sleeksnap.http.HttpUtil;
import org.sleeksnap.upload.TextUpload;
import org.sleeksnap.uploaders.Uploader;

/**
 * An uploader for Laravel's Paste Bucket
 * 
 * @author Nikki
 *
 */
public class LaravelUploader extends Uploader<TextUpload> {

	private static final String LARAVEL_URL = "http://paste.laravel.com/";

	@Override
	public String upload(TextUpload t) throws Exception {
		String url = HttpUtil.executePostForLocation(LARAVEL_URL, "paste=" + HttpUtil.encode(t.getText()));
		if (url == null) {
			throw new Exception("Failed to paste!");
		}
		return url;
	}

	@Override
	public String getName() {
		return "Laravel";
	}

}
