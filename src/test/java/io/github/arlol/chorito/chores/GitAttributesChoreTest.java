package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitAttributesChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new GitAttributesChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		Path gitignore = extension.root().resolve(".gitattributes");
		doit();
		assertThat(FilesSilent.exists(gitignore)).isTrue();
		assertThat(FilesSilent.readString(gitignore)).isEqualTo("""
				*        text=auto eol=lf
				*.bat    text      eol=crlf
				*.cmd    text      eol=crlf
				*.ps1    text      eol=crlf
				*.sh     text      eol=lf
				""");
	}

	@Test
	public void testWithEmptyFile() {
		Path gitignore = extension.root().resolve(".gitattributes");
		FilesSilent.writeString(gitignore, "");
		doit();
		assertThat(FilesSilent.exists(gitignore)).isTrue();
		assertThat(FilesSilent.readString(gitignore)).isEqualTo("""
				*        text=auto eol=lf
				*.bat    text      eol=crlf
				*.cmd    text      eol=crlf
				*.ps1    text      eol=crlf
				*.sh     text      eol=lf
				""");
	}

	@Test
	public void testWithSomething() {
		Path gitignore = extension.root().resolve(".gitattributes");
		FilesSilent.writeString(
				gitignore,
				"""
						# Declare files that will always have LF line endings on checkout.
						*.sh text eol=lf
						"""
		);
		doit();
		assertThat(FilesSilent.exists(gitignore)).isTrue();
		assertThat(FilesSilent.readString(gitignore)).isEqualTo(
				"""
						# Declare files that will always have LF line endings on checkout.
						*.sh text eol=lf
						*        text=auto eol=lf
						*.bat    text      eol=crlf
						*.cmd    text      eol=crlf
						*.ps1    text      eol=crlf
						"""
		);
	}

}
