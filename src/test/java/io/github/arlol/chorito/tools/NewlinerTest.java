package io.github.arlol.chorito.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.github.arlol.chorito.TestPaths;

public class NewlinerTest {

	@Test
	void testMakeAllNewlinesLf() throws Exception {
		testMakeAllNewlines("1\r\n2\n3\n4\r\n", "1\n2\n3\n4\n", "\n");
	}

	@Test
	void testMakeAllNewlinesCrLf() throws Exception {
		testMakeAllNewlines("1\r\n2\n3\n4\r\n", "1\r\n2\r\n3\r\n4\r\n", "\r\n");
	}

	private void testMakeAllNewlines(
			String input,
			String expected,
			String newline
	) throws Exception {
		Path tempFile = TestPaths.tempFile();
		try {
			Files.writeString(tempFile, input);
			Newliner.makeAllNewlines(tempFile, newline);
			String actual = Files.readString(tempFile);
			assertEquals(expected, actual);
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

	@Test
	void testEnsureLfNewlineAtEof() throws Exception {
		testEnsureNewlineAtEof("Hi", "Hi\n", "\n");
	}

	@Test
	void testEnsureCrLfNewlineAtEof() throws Exception {
		testEnsureNewlineAtEof("Hi", "Hi\r\n", "\r\n");
	}

	private void testEnsureNewlineAtEof(
			String input,
			String expected,
			String newline
	) throws Exception {
		Path tempFile = TestPaths.tempFile();
		try {
			Files.writeString(tempFile, input);
			Newliner.ensureNewlineAtEof(tempFile, newline);
			String actual = Files.readString(tempFile);
			assertEquals(expected, actual);
		} finally {
			Files.deleteIfExists(tempFile);
		}
	}

}
