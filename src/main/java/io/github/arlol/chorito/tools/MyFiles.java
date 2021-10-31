package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;

public abstract class MyFiles {

	private MyFiles() {
	}

	public static void writeString(
			Path path,
			CharSequence content,
			OpenOption... options
	) {
		try {
			Path parent = path.getParent();
			if (parent == null) {
				throw new IllegalArgumentException();
			}
			Files.createDirectories(parent);
			Files.writeString(path, content, options);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
