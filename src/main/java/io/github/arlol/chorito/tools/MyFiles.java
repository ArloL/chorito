package io.github.arlol.chorito.tools;

import java.io.IOException;
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
	) throws IOException {
		Files.createDirectories(path.getParent());
		Files.writeString(path, content, options);
	}

}
