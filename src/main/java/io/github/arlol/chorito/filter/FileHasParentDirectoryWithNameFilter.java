package io.github.arlol.chorito.filter;

import java.nio.file.Path;

public abstract class FileHasParentDirectoryWithNameFilter {

	private FileHasParentDirectoryWithNameFilter() {
	}

	public static boolean filter(Path start, String name) {
		for (Path path = start.getParent(); path != null; path = path
				.getParent()) {
			Path fileName = path.getFileName();
			if (fileName != null && fileName.toString().equals(name)) {
				return true;
			}
		}
		return false;
	}

}
