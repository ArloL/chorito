package io.github.arlol.chorito.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.arlol.chorito.TestPaths;

public class FileIsGoneFilterTest {

	@Test
	void testGone() throws Exception {
		test(true, "does-not-exist-openstreetmap.png");
	}

	@Test
	void testBinary() throws Exception {
		test(false, "openstreetmap.png");
	}

	@Test
	void testEmpty() throws Exception {
		test(false, "empty.txt");
	}

	@Test
	void testNotEmpty() throws Exception {
		test(false, "not-empty.txt");
	}

	private void test(boolean expected, String path) throws IOException {
		boolean actual = FileIsGoneFilter.fileIsGone(TestPaths.get(path));
		assertEquals(expected, actual);
	}

}
