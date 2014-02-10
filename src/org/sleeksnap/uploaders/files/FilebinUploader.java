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
package org.sleeksnap.uploaders.files;

import java.util.HashMap;
import java.util.Map;

import org.sleeksnap.http.MultipartPostMethod;
import org.sleeksnap.upload.FileUpload;
import org.sleeksnap.uploaders.Settings;
import org.sleeksnap.uploaders.UploadException;
import org.sleeksnap.uploaders.Uploader;

/**
 * An uploader for http://filebin.ca http://filebin.ca/tools.php
 * 
 * @author Nikki
 * 
 */
@Settings(required = {}, optional = { "api_key" })
public class FilebinUploader extends Uploader<FileUpload> {

	/**
	 * The Filebin API URL
	 */
	private static final String API_URL = "http://filebin.ca/upload.php";

	@Override
	public String getName() {
		return "Filebin.ca";
	}

	@Override
	public String upload(final FileUpload file) throws Exception {
		final MultipartPostMethod post = new MultipartPostMethod(API_URL);
		post.setParameter("file", file.getFile());
		if (settings.has("api_key")) {
			post.setParameter("key", settings.getString("api_key"));
		}
		post.execute();
		final String resp = post.getResponse();
		// Parsing it is not needed, but it's a good idea to make it easy to
		// use.
		final Map<String, String> res = new HashMap<String, String>();
		// The format is simple key:value, with url being a key, status will be
		// 'error' or 'fail' on an error.
		final String[] lines = resp.split("\n");
		for (final String s : lines) {
			final int idx = s.indexOf(':');
			if (idx != -1) {
				res.put(s.substring(0, idx), s.substring(idx + 1));
			}
		}
		if (!res.containsKey("url")) {
			throw new UploadException(res.get("status"));
		}
		return res.get("url");
	}

}
