package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class CodeFormatterProfileChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new CodeFormatterProfileChore(extension.choreContext()).doit();
	}

	@Test
	public void testWithNothing() {
		Path codeFormatterProfile = extension.root()
				.resolve(".settings/code-formatter-profile.xml");
		doit();
		assertThat(FilesSilent.exists(codeFormatterProfile)).isFalse();
	}

	@Test
	public void testWithPomAndNoGitignore() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);

		doit();

		Path codeFormatterProfile = extension.root()
				.resolve(".settings/code-formatter-profile.xml");
		assertThat(FilesSilent.readString(codeFormatterProfile))
				.startsWith("<?xml");
	}

}
