package io.github.arlol.chorito.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.github.arlol.chorito.TestPaths;

public class FileIsEmptyOrBinaryFilterTest {

	@Test
	void testBinary() throws Exception {
		test(true, "openstreetmap.png");
	}

	@Test
	void testEmpty() throws Exception {
		test(true, "empty.txt");
	}

	@Test
	void testNotEmpty() throws Exception {
		test(false, "not-empty.txt");
	}

	private void test(boolean expected, String path) throws IOException {
		boolean actual = FileIsGoneEmptyOrBinaryFilter
				.fileIsGoneEmptyOrBinary(TestPaths.get(path));
		assertEquals(expected, actual);
	}

}
