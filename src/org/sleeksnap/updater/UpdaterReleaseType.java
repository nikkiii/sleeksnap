package org.sleeksnap.updater;

public enum UpdaterReleaseType {
	DEVELOPMENT("latest_development.json"), RECOMMENDED("latest.json");

	private String feedPath;

	private UpdaterReleaseType(final String feedPath) {
		this.feedPath = feedPath;
	}

	public String getFeedPath() {
		return feedPath;
	}
}
