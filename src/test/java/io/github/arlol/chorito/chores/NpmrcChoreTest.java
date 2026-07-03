package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class NpmrcChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new NpmrcChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		doit();

		Path npmrc = extension.choreContext().resolve(".npmrc");
		assertThat(npmrc).doesNotExist();
	}

	@Test
	public void testCreatesNpmrc() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("package.json"));

		doit();

		Path npmrc = extension.choreContext().resolve(".npmrc");
		assertThat(npmrc).content().isEqualTo("min-release-age=7\n");
	}

	@Test
	public void testCreatesNestedNpmrc() throws Exception {
		FilesSilent
				.touch(extension.choreContext().resolve("nested/package.json"));

		doit();

		Path npmrc = extension.choreContext().resolve("nested/.npmrc");
		assertThat(npmrc).content().isEqualTo("min-release-age=7\n");
	}

	@Test
	public void testPreservesExistingSettings() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("package.json"));
		FilesSilent.writeString(
				extension.choreContext().resolve(".npmrc"),
				"registry=https://registry.example.com/\nsave-exact=true\n"
		);

		doit();

		Path npmrc = extension.choreContext().resolve(".npmrc");
		assertThat(npmrc).content().isEqualTo("""
				registry=https://registry.example.com/
				save-exact=true
				min-release-age=7
				""");
	}

	@Test
	public void testBumpsTooLowValue() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("package.json"));
		FilesSilent.writeString(
				extension.choreContext().resolve(".npmrc"),
				"min-release-age=3\n"
		);

		doit();

		Path npmrc = extension.choreContext().resolve(".npmrc");
		assertThat(npmrc).content().isEqualTo("min-release-age=7\n");
	}

	@Test
	public void testKeepsHigherValue() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("package.json"));
		FilesSilent.writeString(
				extension.choreContext().resolve(".npmrc"),
				"min-release-age=14\n"
		);

		doit();

		Path npmrc = extension.choreContext().resolve(".npmrc");
		assertThat(npmrc).content().isEqualTo("min-release-age=14\n");
	}

	@Test
	public void testStability() throws Exception {
		FilesSilent.touch(extension.choreContext().resolve("package.json"));

		doit();
		doit();

		Path npmrc = extension.choreContext().resolve(".npmrc");
		assertThat(npmrc).content().isEqualTo("min-release-age=7\n");
	}

}
