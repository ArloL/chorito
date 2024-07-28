package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class GitChoreContextTest {

	@Test
	void testHasGitHubRemote() throws Exception {
		assertThat(GitChoreContext.newBuilder(".").build().remotes())
				.isNotEmpty();
	}

}
