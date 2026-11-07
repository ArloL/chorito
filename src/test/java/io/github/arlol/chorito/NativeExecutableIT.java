package io.github.arlol.chorito;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class NativeExecutableIT {

	@Test
	void version() throws IOException, InterruptedException {
		String nativeExecutable = System.getProperty("native.executable");
		String expectedArtifactId = System.getProperty("project.artifactId");
		String expectedVersion = System.getProperty("project.version");
		String expected = expectedArtifactId + " version \"" + expectedVersion
				+ "\"";

		Process process = new ProcessBuilder(
				Path.of(nativeExecutable).toAbsolutePath().toString(),
				"--version"
		).start();
		int exitCode = process.waitFor();
		String output = new String(process.getInputStream().readAllBytes())
				.strip();

		assertEquals(0, exitCode);
		assertEquals(expected, output);
	}

	/**
	 * Runs chorito on its own checkout. The workflow's git diff step after the
	 * build fails if anything changed.
	 */
	@Test
	void choresOnItself() throws IOException, InterruptedException {
		String nativeExecutable = System.getProperty("native.executable");

		Process process = new ProcessBuilder(
				Path.of(nativeExecutable).toAbsolutePath().toString()
		).inheritIO().start();

		assertEquals(0, process.waitFor());
	}

}
