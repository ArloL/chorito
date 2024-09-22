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

	public static Optional<Path> getParentPathWithName(
			Path start,
			String name
	) {
		for (Path path = start.getParent(); path != null; path = path
				.getParent()) {
			Path fileName = path.getFileName();
			if (fileName != null && fileName.toString().equals(name)) {
				return Optional.of(path);
			}
		}
		return Optional.empty();
	}

}
