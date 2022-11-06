package io.github.arlol.chorito;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.github.arlol.chorito.tools.FilesSilent;

public class TestPaths {

	public static Path get(String path) throws IOException {
		return Paths.get("./src/test/resources").resolve(path);
	}

	public static Path tempFile(Path root) {
		Path tempDirectory = FilesSilent.createTempDirectory(root, null);
		return FilesSilent.createTempFile(tempDirectory, null, null);
	}

}
