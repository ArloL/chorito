package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class Ec4jChoreTest {

	private static String DEFAULT_EDITORCONFIG = """
			root = true
			[*]
			end_of_line = lf
			insert_final_newline = true
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void test() throws Exception {
		Path editorConfig = extension.root().resolve(".editorconfig");
		FilesSilent.writeString(editorConfig, DEFAULT_EDITORCONFIG);
		Path text = extension.root().resolve("test.txt");
		FilesSilent.writeString(text, "file with a newline\r\n");

		new Ec4jChore().doit(extension.choreContext());

		assertThat(text).content().isEqualTo("file with a newline\n");
	}

	/**
	 * The reason {@link org.ec4j.lint.api.CustomLinterRegistryBuilder
	 * CustomLinterRegistryBuilder} exists: ec4j matches a linter's include
	 * globs against the path it is handed, and chorito hands it
	 * repository-relative paths, so a nested file matches no include and goes
	 * unlinted unless the path is resolved against the repository root first.
	 * <p>
	 * That class reaches into ec4j's package-private types to do it, which ec4j
	 * does not know about and is free to break. A signature change fails the
	 * compile loudly; a behavioural change behind an unchanged signature would
	 * not, so assert the behaviour here rather than trusting the build.
	 */
	@Test
	public void lintsAFileInANestedDirectory() throws Exception {
		FilesSilent.writeString(
				extension.root().resolve(".editorconfig"),
				DEFAULT_EDITORCONFIG
		);
		Path nested = extension.root().resolve("src/main/java/Main.java");
		FilesSilent.writeString(nested, "class Main {}\r\n");

		new Ec4jChore().doit(extension.choreContext());

		assertThat(nested).content().isEqualTo("class Main {}\n");
	}

	/**
	 * The same resolution under a glob that is not the {@code **}/{@code *}
	 * special case: the XML linter includes {@code **}/{@code *.xml} only, so a
	 * nested pom is reindented and the text file beside it -- which no XML
	 * include matches -- is left as it is.
	 */
	@Test
	public void appliesGlobScopedLintersToNestedFiles() throws Exception {
		FilesSilent.writeString(extension.root().resolve(".editorconfig"), """
				root = true
				[*]
				end_of_line = lf
				insert_final_newline = true
				indent_style = tab
				""");
		String spaceIndented = "<project>\n    <name>nested</name>\n</project>\n";
		Path xml = extension.root().resolve("module/nested/pom.xml");
		FilesSilent.writeString(xml, spaceIndented);
		Path text = extension.root().resolve("module/nested/pom.txt");
		FilesSilent.writeString(text, spaceIndented);

		new Ec4jChore().doit(extension.choreContext());

		assertThat(xml).content()
				.isEqualTo("<project>\n\t<name>nested</name>\n</project>\n");
		assertThat(text).content().isEqualTo(spaceIndented);
	}

}
