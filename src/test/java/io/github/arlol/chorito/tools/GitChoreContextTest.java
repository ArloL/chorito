package io.github.arlol.chorito.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class GitChoreContextTest {

	@Test
	void testHasGitHubRemote() throws Exception {
		boolean hasGitHubRemote = GitChoreContext.newBuilder(".")
				.build()
				.hasGitHubRemote();
		assertTrue(hasGitHubRemote);
	}

}
