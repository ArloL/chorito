package io.github.arlol.chorito.filter;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.FilesSilent;

public abstract class FileHasNoParentDirectoryWithFileFilter {

	private FileHasNoParentDirectoryWithFileFilter() {
	}

	public static boolean filter(Path start, String file) {
		for (Path path = start.getParent(); path != null; path = path
				.getParent()) {
			if (FilesSilent.exists(path.resolve(file))) {
				return false;
			}
		}
		return true;
	}

}
