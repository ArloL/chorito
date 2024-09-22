package io.github.arlol.chorito.filter;

import java.nio.file.Path;

public abstract class FileHasParentDirectoryWithNameFilter {

	private FileHasParentDirectoryWithNameFilter() {
	}

	public static boolean filter(Path start, String name) {
		for (Path path = start.getParent(); path != null; path = path
				.getParent()) {
			if (name.equals(path.getFileName().toString())) {
				return true;
			}
		}
		return false;
	}

}
