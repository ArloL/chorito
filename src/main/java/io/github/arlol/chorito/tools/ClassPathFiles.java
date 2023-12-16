package io.github.arlol.chorito.tools;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.CharsetDecoder;

public abstract class ClassPathFiles {

	private ClassPathFiles() {
	}

	public static String readString(String path) {
		try (InputStream stream = newInputStream(path)) {
			return new String(stream.readAllBytes(), UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static InputStream newInputStream(String path) {
		return ClassPathFiles.class.getResourceAsStream(path);
	}

	public static BufferedReader newBufferedReader(String path) {
		CharsetDecoder decoder = UTF_8.newDecoder();
		Reader reader = new InputStreamReader(newInputStream(path), decoder);
		return new BufferedReader(reader);
	}

}
