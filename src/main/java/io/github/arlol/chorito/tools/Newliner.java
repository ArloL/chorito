package io.github.arlol.chorito.tools;

import java.nio.file.Path;

public class Newliner {

	public static final String CARRIAGE_RETURN = "\r";
	public static final String CARRIAGE_RETURN_LINE_FEED = "\r\n";
	public static final String OPTIONAL_CARRIAGE_RETURN_LINE_FEED = "\r?\n";
	public static final String LINE_FEED = "\n";

	public static final byte CARRIAGE_RETURN_CODE = 13;
	public static final byte LINE_FEED_CODE = 10;

	public static void makeAllNewlinesCrLf(Path path) {
		makeAllNewlines(path, CARRIAGE_RETURN_LINE_FEED);
	}

	public static void makeAllNewlinesLf(Path path) {
		makeAllNewlines(path, LINE_FEED);
	}

	/**
	 * assumption: text file and UTF-8 encoded
	 */
	public static void makeAllNewlines(Path path, String newline) {
		String content = FilesSilent.readString(path);
		content = content
				.replaceAll(OPTIONAL_CARRIAGE_RETURN_LINE_FEED, newline);
		FilesSilent.writeString(path, content);
	}

	public static void ensureSystemNewlineAtEof(Path path) {
		ensureNewlineAtEof(path, System.lineSeparator());
	}

	public static void ensureCrlfNewlineAtEof(Path path) {
		ensureNewlineAtEof(path, CARRIAGE_RETURN_LINE_FEED);
	}

	public static void ensureLfNewlineAtEof(Path path) {
		ensureNewlineAtEof(path, LINE_FEED);
	}

	/**
	 * assumption: text file and UTF-8 encoded
	 */
	public static void ensureNewlineAtEof(Path path, String newline) {
		String content = FilesSilent.readString(path);
		if (!content.endsWith(LINE_FEED)) {
			if (content.contains(CARRIAGE_RETURN_LINE_FEED)) {
				content = content + CARRIAGE_RETURN_LINE_FEED;
			} else if (content.contains(LINE_FEED)) {
				content = content + LINE_FEED;
			} else {
				content = content + newline;
			}
			FilesSilent.writeString(path, content);
		}
	}

}
