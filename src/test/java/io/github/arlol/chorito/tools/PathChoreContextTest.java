package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PathChoreContextTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	void testHasGitHubRemote() throws Exception {
		boolean hasGitHubRemote = context().hasGitHubRemote();
		assertFalse(hasGitHubRemote);
	}

	@Test
	void testTextFiles() throws Exception {
		assertThat(context().textFiles()).hasSize(1);
	}

	@Test
	void testFiles() throws Exception {
		assertThat(context().files()).hasSize(2);
	}

	private PathChoreContext context() {
		FileSystem fileSystem = this.extension.getFileSystem();
		Path root = fileSystem.getPath("/");
		FilesSilent
				.writeString(root.resolve("test.txt"), "this is a text file");
		FilesSilent.write(root.resolve("test.bin"), new byte[] { 0 });
		return new PathChoreContext(root);
	}

}
