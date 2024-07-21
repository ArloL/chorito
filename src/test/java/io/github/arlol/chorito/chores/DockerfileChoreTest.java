package io.github.arlol.chorito.chores;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class DockerfileChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new DockerfileChore().doit(extension.choreContext());
	}

	@ParameterizedTest
	@ValueSource(
			strings = { "dOcKeRfIlE", "dockerFile", "dockerFile", "dockerfile",
					"DOCKERfile", "dockerfilE" }
	)
	public void test(String name) throws Exception {
		Path wronglyNamedFile = extension.root().resolve(name);
		FilesSilent.touch(wronglyNamedFile);

		new DockerfileChore().doit(extension.choreContext());

		assertFalse(FilesSilent.exists(wronglyNamedFile));
		assertTrue(
				FilesSilent
						.exists(extension.choreContext().resolve("Dockerfile"))
		);
	}

}
