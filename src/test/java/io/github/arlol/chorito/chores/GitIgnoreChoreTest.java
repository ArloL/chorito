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

	private void doit() {
		new GitIgnoreChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		Path gitignore = extension.root().resolve(".gitignore");
		doit();
		assertThat(FilesSilent.exists(gitignore)).isFalse();
	}

	@Test
	public void testWithPomAndNoGitignore() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);

		doit();

		Path gitignore = extension.root().resolve(".gitignore");
		assertThat(FilesSilent.readString(gitignore)).isEqualTo(".project\n");
	}

	@Test
	public void testWithPomAndExistingGitignore() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);
		Path gitignore = extension.root().resolve(".gitignore");
		FilesSilent.write(gitignore, List.of("lol"), "\n");

		doit();

		assertThat(FilesSilent.readString(gitignore))
				.isEqualTo("lol\n.project\n");
	}

	@Test
	public void testWithPomAndExistingGitignoreWithSettings() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);
		Path gitignore = extension.root().resolve(".gitignore");
		FilesSilent.write(gitignore, List.of(".settings"), "\n");

		doit();

		assertThat(FilesSilent.readString(gitignore))
				.isEqualTo("# .settings\n.project\n");
	}

	@Test
	public void testWithPom() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);

		doit();

		Path settingsGitignore = extension.root()
				.resolve(".settings/.gitignore");
		assertThat(FilesSilent.readString(settingsGitignore)).isEqualTo(
				"*.prefs\n" + "!org.eclipse.jdt.core.prefs\n"
						+ "!org.eclipse.jdt.ui.prefs\n" + ""
		);
	}

}
