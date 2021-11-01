package io.github.arlol.chorito.chores;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class EditorConfigChoreTest {

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
				.removeBracketsFromSingleExtensionGroups(
						Arrays.asList(input.split("\r?\n"))
				)
				.stream()
				.collect(joining("\n", "", "\n"));
		assertEquals(expected, actual);
	}

}
