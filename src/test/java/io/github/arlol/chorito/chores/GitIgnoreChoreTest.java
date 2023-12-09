package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitIgnoreChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		Path pom = extension.root().resolve(".gitignore");
		new GitIgnoreChore(extension.choreContext()).doit();
		assertThat(FilesSilent.exists(pom)).isFalse();
	}

	@Test
	public void testWithPomAndNoGitignore() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);

		new GitIgnoreChore(extension.choreContext()).doit();

		Path gitignore = extension.root().resolve(".gitignore");
		assertThat(FilesSilent.readString(gitignore)).isEqualTo(".project\n");
		assertThat(FilesSilent.exists(gitignore)).isTrue();
	}

	@Test
	public void testWithPomAndExistingGitignore() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);
		Path gitignore = extension.root().resolve(".gitignore");
		FilesSilent.write(gitignore, List.of("lol"), "\n");

		new GitIgnoreChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(gitignore))
				.isEqualTo("lol\n.project\n");
		assertThat(FilesSilent.exists(gitignore)).isTrue();
	}

	@Test
	public void testWithPomAndExistingGitignoreWithSettings() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);
		Path gitignore = extension.root().resolve(".gitignore");
		FilesSilent.write(gitignore, List.of(".settings"), "\n");

		new GitIgnoreChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(gitignore)).isEqualTo(".project\n");
		assertThat(FilesSilent.exists(gitignore)).isTrue();
	}

}
