package org.sleeksnap.uploaders.url;

import org.sleeksnap.http.HttpUtil;
import org.sleeksnap.http.RequestData;
import org.sleeksnap.upload.URLUpload;
import org.sleeksnap.uploaders.UploadException;
import org.sleeksnap.uploaders.Uploader;

/**
 * This is a URL Shortner for po.st, po.st can be used to track the stats of
 * your link. The included API Key is for use by Sleeksnap ONLY, If you would
 * like a key you may register one at po.st's website
 * 
 * @author Andrew Sampson - http://github.com/codeusa
 * 
 */
public class PostShortner extends Uploader<URLUpload> {

	/**
	 * The API KEY and URL
	 */

	private static final String API_KEY = "2560371B-8FE7-4674-BB13-D1840B86CF49";
	private static final String API_URL = "http://po.st/api/shorten";

	@Override
	public String getName() {
		return "po.st";
	}

	@Override
	public String upload(final URLUpload url) throws Exception {
		final RequestData get = new RequestData();
		get.put("longUrl", url.getURL());
		get.put("apiKey", API_KEY);
		get.put("format", "txt");

		final String shortURL = HttpUtil.executeGet(API_URL, get);
		if (shortURL.startsWith("http")) {
			return shortURL;
		}

		throw new UploadException("Unexpected response from server.");
	}

}
