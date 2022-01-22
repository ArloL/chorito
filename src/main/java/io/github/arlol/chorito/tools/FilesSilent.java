package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

public abstract class FilesSilent {

	private FilesSilent() {
	}

	public static void writeString(
			Path path,
			CharSequence content,
			OpenOption... options
	) {
		try {
			Path parent = MyPaths.getParent(path);
			Files.createDirectories(parent);
			Files.writeString(path, content, options);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static boolean exists(Path path) {
		return Files.exists(path);
	}

	public static String readString(Path path) {
		try {
			return Files.readString(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void write(
			Path path,
			Iterable<? extends CharSequence> lines,
			OpenOption... options
	) {
		try {
			Files.write(path, lines, options);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static List<String> readAllLines(Path path) {
		try {
			return Files.readAllLines(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path setPosixFilePermissions(
			Path path,
			Set<PosixFilePermission> perms
	) {
		try {
			return Files.setPosixFilePermissions(path, perms);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Set<PosixFilePermission> getPosixFilePermissions(
			Path path,
			LinkOption... options
	) {
		try {
			return Files.getPosixFilePermissions(path, options);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
