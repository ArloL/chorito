package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class RemoveUnnecessaryExecFlagsChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void test() throws Exception {
		Path runSh = extension.root().resolve("run.sh");
		FilesSilent.writeString(runSh, "#!/bin/sh\necho Hi");
		var permissions = FilesSilent.getPosixFilePermissions(runSh);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		permissions.add(PosixFilePermission.GROUP_EXECUTE);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		FilesSilent.setPosixFilePermissions(runSh, permissions);

		Path noScript = extension.root().resolve("no-script.sh");
		FilesSilent.writeString(noScript, "Just text");
		permissions = FilesSilent.getPosixFilePermissions(noScript);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		permissions.add(PosixFilePermission.GROUP_EXECUTE);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		FilesSilent.setPosixFilePermissions(noScript, permissions);

		Path binary = extension.root().resolve("test.bin");
		FilesSilent.write(binary, new byte[] { (byte) 198 });
		permissions = FilesSilent.getPosixFilePermissions(binary);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		permissions.add(PosixFilePermission.GROUP_EXECUTE);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		FilesSilent.setPosixFilePermissions(binary, permissions);

		new RemoveUnnecessaryExecFlagsChore(extension.choreContext()).doit();

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

		assertThat(FilesSilent.getPosixFilePermissions(binary))
				.contains(PosixFilePermission.OWNER_EXECUTE);
		assertThat(FilesSilent.getPosixFilePermissions(binary))
				.contains(PosixFilePermission.GROUP_EXECUTE);
		assertThat(FilesSilent.getPosixFilePermissions(binary))
				.contains(PosixFilePermission.OTHERS_EXECUTE);
	}

}
