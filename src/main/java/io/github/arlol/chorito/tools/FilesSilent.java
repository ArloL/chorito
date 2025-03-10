package io.github.arlol.chorito.tools;

import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.jspecify.annotations.Nullable;

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

	public static boolean anyNotExists(Path... paths) {
		for (Path path : paths) {
			if (!exists(path)) {
				return true;
			}
		}
		return false;
	}

	public static boolean anyExists(Path... paths) {
		for (Path path : paths) {
			if (exists(path)) {
				return true;
			}
		}
		return false;
	}

	public static boolean noneExists(Path... paths) {
		for (Path path : paths) {
			if (exists(path)) {
				return false;
			}
		}
		return true;
	}

	public static boolean exists(Path path) {
		return Files.exists(path);
	}

	public static boolean anyChildExists(Path root, String... paths) {
		for (String path : paths) {
			if (exists(root.resolve(path))) {
				return true;
			}
		}
		return false;
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
			String lineSeperator,
			OpenOption... options
	) {
		String string = StreamSupport.stream(lines.spliterator(), false)
				.collect(joining(lineSeperator));
		if (!string.isEmpty() && !string.substring(string.length() - 1)
				.equals(lineSeperator)) {
			string += lineSeperator;
		}
		writeString(path, string, options);
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

	public static void setPosixFilePermissions(
			PosixFileAttributeView view,
			Set<PosixFilePermission> permissions
	) {
		try {
			view.setPermissions(permissions);
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

	public static Set<PosixFilePermission> getPosixFilePermissions(
			PosixFileAttributeView view
	) {
		try {
			return view.readAttributes().permissions();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static boolean deleteIfExists(Path path) {
		try {
			return Files.deleteIfExists(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void delete(Path path) {
		try {
			Files.delete(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Stream<Path> walk(Path start, FileVisitOption... options) {
		try {
			return Files.walk(start);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path write(Path path, byte[] bytes, OpenOption... options) {
		try {
			return Files.write(path, bytes, options);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path move(Path source, Path target, CopyOption... options) {
		try {
			return Files.move(source, target, options);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path touch(Path path, OpenOption... options) {
		try {
			if (exists(path)) {
				Files.setLastModifiedTime(path, FileTime.from(Instant.now()));
				return path;
			} else {
				Path parent = MyPaths.getParent(path);
				Files.createDirectories(parent);
				return Files.createFile(path);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}

	public static InputStream newInputStream(Path path, OpenOption... options) {
		try {
			return Files.newInputStream(path, options);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static boolean isRegularFile(Path path, LinkOption... options) {
		return Files.isRegularFile(path, options);
	}

	public static Path createTempDirectory(
			Path dir,
			String prefix,
			FileAttribute<?>... attrs
	) {
		try {
			return Files.createTempDirectory(dir, prefix, attrs);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path createTempFile(
			Path dir,
			String prefix,
			String suffix,
			FileAttribute<?>... attrs
	) {
		try {
			return Files.createTempFile(dir, prefix, suffix, attrs);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static byte[] readAllBytes(Path path) {
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Path createDirectories(Path dir, FileAttribute<?>... attrs) {
		try {
			return Files.createDirectories(dir, attrs);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static OutputStream newOutputStream(
			Path path,
			OpenOption... options
	) {
		try {
			return Files.newOutputStream(path, options);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static BufferedReader newBufferedReader(Path path) {
		try {
			return Files.newBufferedReader(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static <V extends FileAttributeView> @Nullable V getFileAttributeView(
			Path path,
			Class<V> type,
			LinkOption... options
	) {
		return Files.getFileAttributeView(path, type);
	}

}
