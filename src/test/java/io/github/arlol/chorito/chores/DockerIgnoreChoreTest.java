package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class DockerIgnoreChoreTest {

	private static final String DEFAULT_MAVEN = """
			# Created by chorito https://github.com/ArloL/chorito

			Dockerfile
			.dockerignore

			target/

			# End of chorito. Add your ignores after this line and they will be preserved.
			""";
	private static final String DEFAULT_GRADLE = """
			# Created by chorito https://github.com/ArloL/chorito

			Dockerfile
			.dockerignore

			build/
			.gradle/

			# End of chorito. Add your ignores after this line and they will be preserved.
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new DockerIgnoreChore().doit(extension.choreContext());
	}

	@Test
	public void testMavenDockerIgnore() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("pom.xml"));
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));

		new DockerIgnoreChore().doit(extension.choreContext());

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertTrue(FilesSilent.exists(dockerIgnore));
		assertThat(FilesSilent.readString(dockerIgnore))
				.isEqualTo(DEFAULT_MAVEN);
	}

	@Test
	public void testGradleDockerIgnore() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("build.gradle"));
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));

		new DockerIgnoreChore().doit(extension.choreContext());

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertTrue(FilesSilent.exists(dockerIgnore));
		assertThat(FilesSilent.readString(dockerIgnore))
				.isEqualTo(DEFAULT_GRADLE);
	}

}