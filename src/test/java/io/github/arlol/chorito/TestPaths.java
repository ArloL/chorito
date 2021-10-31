package io.github.arlol.chorito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestPaths {

	public static Path get(String path) throws IOException {
		return Paths.get("./src/test/resources").resolve(path);
	}

	public static Path tempFile() throws IOException {
		Path tempDirectory = Files
				.createTempDirectory(Paths.get("target"), null);
		tempDirectory.toFile().deleteOnExit();
		return Files.createTempFile(tempDirectory, null, null);
	}

}
