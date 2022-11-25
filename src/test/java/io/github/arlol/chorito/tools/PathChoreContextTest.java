package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PathChoreContextTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	void testHasGitHubRemote() throws Exception {
		boolean hasGitHubRemote = extension.choreContext().hasGitHubRemote();
		assertFalse(hasGitHubRemote);
	}

	@Test
	void testTextFiles() throws Exception {
		assertThat(extension.choreContext().textFiles()).hasSize(1);
	}

	@Test
	void testFiles() throws Exception {
		assertThat(extension.choreContext().files()).hasSize(2);
	}

	@Test
	void testRefresh() throws Exception {
		ChoreContext choreContext = extension.choreContext();

		FilesSilent.deleteIfExists(extension.root().resolve("test.txt"));

		assertThat(choreContext.textFiles()).hasSize(1);
		assertThat(choreContext.refresh().textFiles()).hasSize(0);
	}

	@BeforeEach
	public void setup() {
		FilesSilent.writeString(
				extension.root().resolve("test.txt"),
				"this is a text file"
		);
		FilesSilent
				.write(extension.root().resolve("test.bin"), new byte[] { 0 });
	}

}
