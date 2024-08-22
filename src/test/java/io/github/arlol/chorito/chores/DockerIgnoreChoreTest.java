package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class DockerIgnoreChoreTest {

	private static final String DEFAULT_MAVEN = """
			### Docker ###

			Dockerfile
			.dockerignore

			### Maven ###

			/target/

			# Add custom ignores after this line to be preserved during automated updates
			""";
	private static final String DEFAULT_GRADLE = """
			### Docker ###

			Dockerfile
			.dockerignore

			### Gradle ###

			/build/
			/.gradle/

			# Add custom ignores after this line to be preserved during automated updates
			""";
	private static final String OLD_MAVEN = """
			# Created by chorito https://github.com/ArloL/chorito

			### Docker ###

			Dockerfile
			.dockerignore

			### Maven ###

			/target/

			# End of chorito. Add your ignores after this line and they will be preserved.
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new DockerIgnoreChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		doit();
	}

	@Test
	public void testMavenDockerIgnore() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("pom.xml"));
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content().isEqualTo(DEFAULT_MAVEN);
	}

	@Test
	public void testGradleDockerIgnore() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("build.gradle"));
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content().isEqualTo(DEFAULT_GRADLE);
	}

	@Test
	public void testDockerComposeYml() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));
		FilesSilent
				.touch(extension.choreContext().resolve("docker-compose.yml"));

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content().contains("docker-compose.yml");
	}

	@Test
	public void testDockerComposeYaml() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));
		FilesSilent
				.touch(extension.choreContext().resolve("docker-compose.yaml"));

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content().contains("docker-compose.yaml");
	}

	@Test
	public void testComposeYml() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));
		FilesSilent.touch(extension.choreContext().resolve("compose.yml"));

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content().contains("compose.yml");
	}

	@Test
	public void testComposeYaml() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));
		FilesSilent.touch(extension.choreContext().resolve("compose.yaml"));

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content()
				.isEqualTo(
						"""
								### Docker ###

								Dockerfile
								.dockerignore
								compose.yaml

								# Add custom ignores after this line to be preserved during automated updates
								"""
				);
	}

	@Test
	public void testAllComposeNames() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));
		FilesSilent.touch(extension.choreContext().resolve("compose.yaml"));
		FilesSilent.touch(extension.choreContext().resolve("compose.yml"));
		FilesSilent
				.touch(extension.choreContext().resolve("docker-compose.yml"));
		FilesSilent
				.touch(extension.choreContext().resolve("docker-compose.yaml"));

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content()
				.isEqualTo(
						"""
								### Docker ###

								Dockerfile
								.dockerignore
								compose.yaml
								compose.yml
								docker-compose.yml
								docker-compose.yaml

								# Add custom ignores after this line to be preserved during automated updates
								"""
				);
	}

	@Test
	public void testPackageJson() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));
		FilesSilent.touch(extension.choreContext().resolve("package.json"));

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content().contains("/node_modules/");
	}

	@Test
	public void testNestedPackageJson() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));
		FilesSilent
				.touch(extension.choreContext().resolve("nested/package.json"));

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content().contains("/nested/node_modules/");
	}

	@Test
	public void testDockerignoreOnly() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("build.gradle"));
		FilesSilent.touch(extension.choreContext().resolve(".dockerignore"));

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content().isEqualTo(DEFAULT_GRADLE);
	}

	@Test
	public void testUpdatingSuffixPrefix() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("pom.xml"));
		FilesSilent.writeString(
				extension.choreContext().resolve(".dockerignore"),
				OLD_MAVEN
		);

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content().isEqualTo(DEFAULT_MAVEN);
	}

	@Test
	public void testPreservingCustomStuff() throws Exception {
		var dockerignore = OLD_MAVEN + "\n\ndeployment\ncharts\n";
		FilesSilent.touch(extension.choreContext().resolve("pom.xml"));
		FilesSilent.writeString(
				extension.choreContext().resolve(".dockerignore"),
				dockerignore
		);

		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content()
				.isEqualTo(DEFAULT_MAVEN + "\n\ndeployment\ncharts\n");
	}

	@Test
	public void testStability() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("pom.xml"));
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));

		doit();
		doit();

		Path dockerIgnore = extension.choreContext().resolve(".dockerignore");
		assertThat(dockerIgnore).content().isEqualTo(DEFAULT_MAVEN);
	}

}
