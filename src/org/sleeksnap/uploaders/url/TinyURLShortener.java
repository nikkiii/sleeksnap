/**
 * Sleeksnap, the open source cross-platform screenshot uploader
 * Copyright (C) 2014 Nikki <nikki@nikkii.us>
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
package org.sleeksnap.uploaders.url;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sleeksnap.http.HttpUtil;
import org.sleeksnap.http.RequestData;
import org.sleeksnap.upload.URLUpload;
import org.sleeksnap.uploaders.UploadException;
import org.sleeksnap.uploaders.Uploader;

/**
 * A URL Shortener for TinyURL
 * 
 * @author Nikki
 * 
 */
public class TinyURLShortener extends Uploader<URLUpload> {

	/**
	 * The page URL
	 */
	private static final String PAGE_URL = "http://tinyurl.com/create.php";

	/**
	 * The pattern to find the shortened url
	 */
	private static final Pattern urlPattern = Pattern
			.compile("<blockquote><b>(.*?)</b>");

	@Override
	public String getName() {
		return "Tinyurl";
	}

	@Override
	public String upload(final URLUpload url) throws Exception {
		final RequestData data = new RequestData();

		data.put("url", url.getURL());

		final String contents = HttpUtil.executeGet(PAGE_URL, data);

		final Matcher matcher = urlPattern.matcher(contents);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			throw new UploadException("Cannot find the short url");
		}
	}
}
