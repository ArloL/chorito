package io.github.arlol.chorito.filter;

import java.nio.file.Files;
import java.nio.file.Path;

public abstract class FileIsGoneFilter {

	private FileIsGoneFilter() {
	}

	public static boolean fileIsGone(Path path) {
		return !Files.exists(path);
	}

}
