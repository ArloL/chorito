package io.github.arlol.chorito.chores;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class DockerfileChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new DockerfileChore(extension.choreContext()).doit();
	}

	@ParameterizedTest
	@ValueSource(
			strings = { "dOcKeRfIlE", "dockerFile", "dockerFile", "dockerfile",
					"DOCKERfile", "dockerfilE" }
	)
	public void test(String name) throws Exception {
		ChoreContext context = extension.choreContext();

		Path wronglyNamedFile = context.resolve(name);
		FilesSilent.writeString(wronglyNamedFile, "");

		new DockerfileChore(context.refresh()).doit();

		assertFalse(FilesSilent.exists(wronglyNamedFile));
		assertTrue(FilesSilent.exists(context.resolve("Dockerfile")));
	}

}
