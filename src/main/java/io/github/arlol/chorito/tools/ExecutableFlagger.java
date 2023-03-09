package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;

public abstract class ExecutableFlagger {

	public ExecutableFlagger() {
	}

	public static void makeExecutableIfPossible(Path path) {
		try {
			PosixFileAttributeView view = Files
					.getFileAttributeView(path, PosixFileAttributeView.class);
			if (view != null) {
				var permissions = view.readAttributes().permissions();
				permissions.add(PosixFilePermission.OWNER_EXECUTE);
				permissions.add(PosixFilePermission.GROUP_EXECUTE);
				permissions.add(PosixFilePermission.OTHERS_EXECUTE);
				view.setPermissions(permissions);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void makeNotExecutableIfPossible(Path path) {
		try {
			PosixFileAttributeView view = Files
					.getFileAttributeView(path, PosixFileAttributeView.class);
			if (view != null) {
				var permissions = view.readAttributes().permissions();
				permissions.remove(PosixFilePermission.OWNER_EXECUTE);
				permissions.remove(PosixFilePermission.GROUP_EXECUTE);
				permissions.remove(PosixFilePermission.OTHERS_EXECUTE);
				view.setPermissions(permissions);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static boolean isExecutable(Path path) {
		try {
			PosixFileAttributeView view = Files
					.getFileAttributeView(path, PosixFileAttributeView.class);
			if (view != null) {
				var permissions = view.readAttributes().permissions();
				if (permissions.contains(PosixFilePermission.OWNER_EXECUTE)
						|| permissions
								.contains(PosixFilePermission.GROUP_EXECUTE)
						|| permissions
								.contains(PosixFilePermission.OTHERS_EXECUTE)) {
					return true;
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return false;
	}

}
