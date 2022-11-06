package io.github.arlol.chorito.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.SmallPng;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class FileIsGoneOrBinaryFilterTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	void testGone() throws Exception {
		test(true, "does-not-exist");
	}

	@Test
	void testBinary() throws Exception {
		test(true, "image.png");
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
		Path root = extension.root();
		FilesSilent.touch(root.resolve("empty.txt"));
		FilesSilent.writeString(root.resolve("not-empty.txt"), "Hi");
		FilesSilent.write(root.resolve("image.png"), SmallPng.BYTES);

		boolean actual = FileIsGoneOrBinaryFilter
				.fileIsGoneOrBinary(root.resolve(path));

		assertEquals(expected, actual);
	}

}
