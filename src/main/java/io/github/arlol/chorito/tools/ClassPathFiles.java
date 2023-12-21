package io.github.arlol.chorito.tools;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;

import io.github.arlol.chorito.Main;

public abstract class ClassPathFiles {

	private ClassPathFiles() {
	}

	public static byte[] readAllBytes(String path) {
		try (InputStream stream = newInputStream(path)) {
			return stream.readAllBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String readString(String path) {
		return readString(path, UTF_8);
	}

	public static String readString(String path, Charset cs) {
		return new String(readAllBytes(path), cs);
	}

	public static List<String> readAllLines(String path, Charset cs) {
		try (BufferedReader reader = newBufferedReader(path, cs)) {
			List<String> result = new ArrayList<>();
			for (;;) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				result.add(line);
			}
			return result;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static List<String> readAllLines(String path) {
		return readAllLines(path, UTF_8);
	}

	public static InputStream newInputStream(String path) {
		InputStream inputStream = Main.class.getResourceAsStream(path);
		if (inputStream == null) {
			throw new IllegalStateException("Could not find " + path);
		}
		return inputStream;
	}

	public static BufferedReader newBufferedReader(String path) {
		return newBufferedReader(path, UTF_8);
	}

	public static BufferedReader newBufferedReader(String path, Charset cs) {
		CharsetDecoder decoder = cs.newDecoder();
		Reader reader = new InputStreamReader(newInputStream(path), decoder);
		return new BufferedReader(reader);
	}

}
