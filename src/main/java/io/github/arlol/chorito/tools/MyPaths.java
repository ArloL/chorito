package io.github.arlol.chorito.tools;

import java.nio.file.Path;

public abstract class MyPaths {

	private MyPaths() {
	}

	public static Path getFileName(Path path) {
		Path fileName = path.getFileName();
		if (fileName == null) {
			throw new IllegalStateException();
		}
		return fileName;
	}

	public static Path getParent(Path path) {
		Path parent = path.getParent();
		if (parent == null) {
			throw new IllegalStateException();
		}
		return parent;
	}

}
