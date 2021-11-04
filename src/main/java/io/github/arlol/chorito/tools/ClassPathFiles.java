package io.github.arlol.chorito.tools;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public abstract class ClassPathFiles {

	private ClassPathFiles() {
	}

	public static String readString(String path) {
		try (InputStream stream = ClassPathFiles.class
				.getResourceAsStream(path)) {
			return new String(stream.readAllBytes(), UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
