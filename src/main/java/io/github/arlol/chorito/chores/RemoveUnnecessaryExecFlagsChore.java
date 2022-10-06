package io.github.arlol.chorito.chores;

import java.nio.file.attribute.PosixFilePermission;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class RemoveUnnecessaryExecFlagsChore {

	private final ChoreContext context;

	public RemoveUnnecessaryExecFlagsChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		context.textFiles().stream().forEach(path -> {
			var permissions = FilesSilent.getPosixFilePermissions(path);
			String readString = FilesSilent.readString(path);
			if (!readString.startsWith("#!")) {
				if (permissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
					permissions.remove(PosixFilePermission.OWNER_EXECUTE);
				}
				if (permissions.contains(PosixFilePermission.GROUP_EXECUTE)) {
					permissions.remove(PosixFilePermission.GROUP_EXECUTE);
				}
				if (permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
					permissions.remove(PosixFilePermission.OTHERS_EXECUTE);
				}
				FilesSilent.setPosixFilePermissions(path, permissions);
			}
		});
	}

}
