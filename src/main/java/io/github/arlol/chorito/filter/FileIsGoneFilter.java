package io.github.arlol.chorito.filter;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.FilesSilent;

public abstract class FileIsGoneFilter {

	private FileIsGoneFilter() {
	}

	public static boolean fileIsGone(Path path) {
		return !FilesSilent.exists(path);
	}

}
