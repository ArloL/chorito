package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class FilesSilentTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWriteWithNothing() {
		// given
		Path path = extension.root().resolve("file.txt");

		// when
		FilesSilent.write(path, List.of(), "\n");

		// then
		assertThat(path).content().isEqualTo("");
	}

	@Test
	public void testWriteWithOne() {
		// given
		Path path = extension.root().resolve("file.txt");

		// when
		FilesSilent.write(path, List.of("hi"), "\n");

		// then
		assertThat(path).content().isEqualTo("hi\n");
	}

	@Test
	public void testWriteWithTwo() {
		// given
		Path path = extension.root().resolve("file.txt");

		// when
		FilesSilent.write(path, List.of("hi", "there"), "\n");

		// then
		assertThat(path).content().isEqualTo("hi\nthere\n");
	}

	@Test
	public void testWriteWithOneWithNewlines() {
		// given
		Path path = extension.root().resolve("file.txt");

		// when
		FilesSilent.write(path, List.of("""
				hi
				""", "there\n", ""), "\n");

		// then
		assertThat(path).content().isEqualTo("hi\n\nthere\n\n");
	}

}
