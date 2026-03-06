package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitAttributesChoreTest {

	private static final String DEFAULT_GITATTRIBUTES = """
			# See https://git-scm.com/docs/gitattributes for more about gitattributes files.

			*        text=auto eol=lf
			*.bat    text      eol=crlf
			*.cmd    text      eol=crlf
			*.ps1    text      eol=crlf
			*.sh     text      eol=lf

			# Add custom entries after this line to be preserved during automated updates
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new GitAttributesChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		Path gitattributes = extension.root().resolve(".gitattributes");
		doit();
		assertThat(FilesSilent.exists(gitattributes)).isTrue();
		assertThat(gitattributes).content().isEqualTo(DEFAULT_GITATTRIBUTES);
	}

	@Test
	public void testWithEmptyFile() {
		Path gitattributes = extension.root().resolve(".gitattributes");
		FilesSilent.writeString(gitattributes, "");
		doit();
		assertThat(FilesSilent.exists(gitattributes)).isTrue();
		assertThat(gitattributes).content().isEqualTo(DEFAULT_GITATTRIBUTES);
	}

	@Test
	public void testWithSomethingAndNoMarker() {
		Path gitattributes = extension.root().resolve(".gitattributes");
		FilesSilent.writeString(
				gitattributes,
				"""
						# Declare files that will always have LF line endings on checkout.
						*.sh text eol=lf
						"""
		);
		doit();
		assertThat(FilesSilent.exists(gitattributes)).isTrue();
		assertThat(gitattributes).content().isEqualTo(DEFAULT_GITATTRIBUTES);
	}

	@Test
	public void testWithCustomContentPreserved() {
		Path gitattributes = extension.root().resolve(".gitattributes");
		FilesSilent.write(
				gitattributes,
				List.of(
						"# Add custom entries after this line to be preserved during automated updates",
						"*.sql  text      eol=lf"
				),
				"\n"
		);
		doit();
		assertThat(FilesSilent.exists(gitattributes)).isTrue();
		assertThat(gitattributes).content()
				.isEqualTo(
						"""
								# See https://git-scm.com/docs/gitattributes for more about gitattributes files.

								*        text=auto eol=lf
								*.bat    text      eol=crlf
								*.cmd    text      eol=crlf
								*.ps1    text      eol=crlf
								*.sh     text      eol=lf

								# Add custom entries after this line to be preserved during automated updates
								*.sql  text      eol=lf
								"""
				);
	}

	@Test
	public void testStability() {
		doit();
		doit();
		assertThat(extension.root().resolve(".gitattributes")).content()
				.isEqualTo(DEFAULT_GITATTRIBUTES);
	}

}
