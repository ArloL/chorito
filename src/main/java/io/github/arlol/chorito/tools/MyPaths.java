package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.Optional;

public abstract class MyPaths {

	private MyPaths() {
	}

	public static String getFileNameAsString(Path path) {
		return Optional.ofNullable(path.getFileName()).orElseThrow().toString();
	}

	public static Path getParent(Path path) {
		Path parent = path.getParent();
		if (parent == null) {
			throw new IllegalStateException();
		}
		return parent;
	}

}
