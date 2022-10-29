package io.github.arlol.chorito.chores;

import java.io.UncheckedIOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.attribute.PosixFilePermission;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class RemoveUnnecessaryExecFlagsChore {

	private final ChoreContext context;

	public RemoveUnnecessaryExecFlagsChore(ChoreContext context) {
		this.context = context.refresh();
	}

	public void doit() {
		context.textFiles().forEach(path -> {
			var permissions = FilesSilent.getPosixFilePermissions(path);
			if (permissions.contains(PosixFilePermission.OWNER_EXECUTE)
					|| permissions.contains(PosixFilePermission.GROUP_EXECUTE)
					|| permissions
							.contains(PosixFilePermission.OTHERS_EXECUTE)) {
				try {
					String readString = FilesSilent.readString(path);
					if (!readString.startsWith("#!")) {
						permissions.remove(PosixFilePermission.OWNER_EXECUTE);
						permissions.remove(PosixFilePermission.GROUP_EXECUTE);
						permissions.remove(PosixFilePermission.OTHERS_EXECUTE);
						FilesSilent.setPosixFilePermissions(path, permissions);
					}
				} catch (UncheckedIOException e) {
					if (!(e.getCause() instanceof MalformedInputException)) {
						throw e;
					}
				}
			}
		});
	}

}
