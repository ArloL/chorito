package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;

public abstract class ExecutableFlagger {

	public ExecutableFlagger() {
	}

	public static void makeExecutableIfPossible(Path path) {
		var view = FilesSilent
				.getFileAttributeView(path, PosixFileAttributeView.class);
		if (view != null) {
			var permissions = FilesSilent.getPosixFilePermissions(view);
			permissions.add(PosixFilePermission.OWNER_EXECUTE);
			permissions.add(PosixFilePermission.GROUP_EXECUTE);
			permissions.add(PosixFilePermission.OTHERS_EXECUTE);
			FilesSilent.setPosixFilePermissions(view, permissions);
		}
	}

	public static void makeNotExecutableIfPossible(Path path) {
		var view = FilesSilent
				.getFileAttributeView(path, PosixFileAttributeView.class);
		if (view != null) {
			var permissions = FilesSilent.getPosixFilePermissions(view);
			permissions.remove(PosixFilePermission.OWNER_EXECUTE);
			permissions.remove(PosixFilePermission.GROUP_EXECUTE);
			permissions.remove(PosixFilePermission.OTHERS_EXECUTE);
			FilesSilent.setPosixFilePermissions(view, permissions);
		}
	}

	public static boolean isExecutable(Path path) {
		var view = FilesSilent
				.getFileAttributeView(path, PosixFileAttributeView.class);
		if (view != null) {
			var permissions = FilesSilent.getPosixFilePermissions(view);
			if (permissions.contains(PosixFilePermission.OWNER_EXECUTE)
					|| permissions.contains(PosixFilePermission.GROUP_EXECUTE)
					|| permissions
							.contains(PosixFilePermission.OTHERS_EXECUTE)) {
				return true;
			}
		}
		return false;
	}

}
