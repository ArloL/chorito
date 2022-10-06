package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.PathChoreContext;

public class RemoveUnnecessaryExecFlagsChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void test() throws Exception {
		ChoreContext context = context();

		Path runSh = context.resolve("run.sh");
		Path noScript = context.resolve("no-script.sh");

		assertThat(FilesSilent.getPosixFilePermissions(runSh))
				.contains(PosixFilePermission.OWNER_EXECUTE);
		assertThat(FilesSilent.getPosixFilePermissions(runSh))
				.contains(PosixFilePermission.GROUP_EXECUTE);
		assertThat(FilesSilent.getPosixFilePermissions(runSh))
				.contains(PosixFilePermission.OTHERS_EXECUTE);

		assertThat(FilesSilent.getPosixFilePermissions(noScript))
				.contains(PosixFilePermission.OWNER_EXECUTE);
		assertThat(FilesSilent.getPosixFilePermissions(noScript))
				.contains(PosixFilePermission.GROUP_EXECUTE);
		assertThat(FilesSilent.getPosixFilePermissions(noScript))
				.contains(PosixFilePermission.OTHERS_EXECUTE);

		new RemoveUnnecessaryExecFlagsChore(context).doit();

		assertThat(FilesSilent.getPosixFilePermissions(runSh))
				.contains(PosixFilePermission.OWNER_EXECUTE);
		assertThat(FilesSilent.getPosixFilePermissions(runSh))
				.contains(PosixFilePermission.GROUP_EXECUTE);
		assertThat(FilesSilent.getPosixFilePermissions(runSh))
				.contains(PosixFilePermission.OTHERS_EXECUTE);

		assertThat(FilesSilent.getPosixFilePermissions(noScript))
				.doesNotContain(PosixFilePermission.OWNER_EXECUTE);
		assertThat(FilesSilent.getPosixFilePermissions(noScript))
				.doesNotContain(PosixFilePermission.GROUP_EXECUTE);
		assertThat(FilesSilent.getPosixFilePermissions(noScript))
				.doesNotContain(PosixFilePermission.OTHERS_EXECUTE);
	}

	private ChoreContext context() {
		FileSystem fileSystem = this.extension.getFileSystem();
		Path root = fileSystem.getPath("/");

		Path runSh = root.resolve("run.sh");
		FilesSilent.writeString(runSh, "#!/bin/sh\necho Hi");
		var permissions = FilesSilent.getPosixFilePermissions(runSh);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		permissions.add(PosixFilePermission.GROUP_EXECUTE);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		FilesSilent.setPosixFilePermissions(runSh, permissions);

		Path noScript = root.resolve("no-script.sh");
		FilesSilent.writeString(noScript, "Just text");
		permissions = FilesSilent.getPosixFilePermissions(noScript);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		permissions.add(PosixFilePermission.GROUP_EXECUTE);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		FilesSilent.setPosixFilePermissions(noScript, permissions);

		return new PathChoreContext(root);
	}

}
