package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;

public class GitMasterBranchChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new GitMasterBranchChore().doit(extension.choreContext());

		assertThat(extension.relativePaths()).isEmpty();
	}

}
