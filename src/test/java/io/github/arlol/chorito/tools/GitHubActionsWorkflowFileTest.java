package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class GitHubActionsWorkflowFileTest {

	@Test
	public void removeVersionsStripsShaAndTag() {
		assertThat(
				GitHubActionsWorkflowFile.removeVersions(
						"jobs:\n  build:\n    steps:\n      - uses: actions/checkout@11bd719 # v5.0.0\n"
				)
		).isEqualTo(
				"jobs:\n  build:\n    steps:\n      - uses: actions/checkout@\n\n"
		);
	}

	@Test
	public void removeVersionsKeepsLinesWithoutUses() {
		String input = "name: main\n\n\n   \n- - -\nrun: echo a@b\n";
		assertThat(GitHubActionsWorkflowFile.removeVersions(input))
				.isEqualTo(input);
	}

	@Test
	public void removeVersionsCutsAtFirstAt() {
		assertThat(
				GitHubActionsWorkflowFile
						.removeVersions("      - uses: a/b@c # d@e\n")
		).isEqualTo("      - uses: a/b@\n\n");
	}

	@Test
	public void removeVersionsHandlesBlankLinesBeforeUses() {
		assertThat(
				GitHubActionsWorkflowFile
						.removeVersions("\n\n      - uses: a/b@c\n")
		).isEqualTo("\n\n      - uses: a/b@\n\n");
	}

}
