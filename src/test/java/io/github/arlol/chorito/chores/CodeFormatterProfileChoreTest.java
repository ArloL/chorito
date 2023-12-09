package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class CodeFormatterProfileChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new CodeFormatterProfileChore().doit(extension.choreContext());
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

		Path jdtCorePrefs = extension.root()
				.resolve(".settings/org.eclipse.jdt.core.prefs");
		List<String> jdtCorePrefsLines = FilesSilent.readAllLines(jdtCorePrefs);
		assertThat(jdtCorePrefsLines).first()
				.isEqualTo("eclipse.preferences.version=1");
		assertThat(jdtCorePrefsLines).contains(
				"org.eclipse.jdt.core.formatter.disabling_tag=@formatter\\:off"
		);
	}

}
