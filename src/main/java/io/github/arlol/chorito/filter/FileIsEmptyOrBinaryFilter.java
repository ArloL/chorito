package io.github.arlol.chorito.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class FileIsEmptyOrBinaryFilter {

	private FileIsEmptyOrBinaryFilter() {
	}

	public static boolean fileIsGoneEmptyOrBinary(Path path) {
		if (!Files.exists(path)) {
			return true;
		}
		try (InputStream inputStream = Files.newInputStream(path)) {
			// this matches git's own binary detection algorithm
			byte[] bytes = inputStream.readNBytes(8000);
			// file is empty
			if (bytes.length == 0) {
				return true;
			}
			for (byte element : bytes) {
				// file is binary
				if (element == 0) {
					return true;
				}
			}
			return false;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
