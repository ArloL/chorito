package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class EditorConfigChoreTest {

	private static String DEFAULT_EDITORCONFIG = """
			# https://editorconfig.org

			root = true

			[*]
			end_of_line = lf
			insert_final_newline = true

			[*.{bat,cmd,ps1}]
			end_of_line = crlf

			[*.sh]
			end_of_line = lf
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new EditorConfigChore(extension.choreContext()).doit();
	}

	@Test
	public void test() throws Exception {
		Path editorConfig = extension.root().resolve(".editorconfig");

		new EditorConfigChore(extension.choreContext()).doit();

		assertTrue(FilesSilent.exists(editorConfig));
		assertThat(FilesSilent.readString(editorConfig))
				.isEqualTo(DEFAULT_EDITORCONFIG);
	}

	@Test
	void testSingleGroup() throws Exception {
		String input = """
				[*.{sh}]
				end_of_line = lf
				""";
		String expected = """
				[*.sh]
				end_of_line = lf
				""";
		test(input, expected);
	}

	@Test
	void testMultipleGroup() throws Exception {
		String input = """
				[*.{bat,cmd,ps1}]
				end_of_line = lf
				""";
		String expected = """
				[*.{bat,cmd,ps1}]
				end_of_line = lf
				""";
		test(input, expected);
	}

	private void test(String input, String expected) {
		var actual = EditorConfigChore
				.removeBracketsFromSingleExtensionGroups(input);
		assertEquals(expected, actual);
	}

}
