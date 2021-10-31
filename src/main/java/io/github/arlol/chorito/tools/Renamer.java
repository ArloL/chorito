package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Renamer {

	private Renamer() {
	}

	public static void replaceInFilename(
			Path path,
			String target,
			String replacement
	) {
		try {
			Path fileName = path.getFileName();
			if (fileName == null) {
				throw new IllegalArgumentException();
			}
			Files.move(
					path,
					path.resolveSibling(
							fileName.toString().replace(".yml", ".yaml")
					)
			);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
