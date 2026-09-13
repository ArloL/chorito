package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

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

	@Test
	public void pinTemurinJavaVersionReplacesToolVersionsFile() {
		var workflow = new GitHubActionsWorkflowFile("""
				jobs:
				  analyze:
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: temurin
				        java-version-file: .tool-versions
				""");

		workflow.pinTemurinJavaVersion("1.2.3");

		assertThat(workflow.asString()).isEqualTo("""
				jobs:
				  analyze:
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: temurin
				        # renovate: datasource=java-version depName=java
				        java-version: 1.2.3
				""");
	}

	@Test
	public void pinTemurinJavaVersionLeavesGraalvmSteps() {
		String input = """
				jobs:
				  linux:
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: graalvm
				        java-version-file: .tool-versions
				""";
		var workflow = new GitHubActionsWorkflowFile(input);

		workflow.pinTemurinJavaVersion("1.2.3");

		assertThat(workflow.asString()).isEqualTo(input);
	}

	@Test
	public void pinTemurinJavaVersionSetsTheVersionItIsGiven() {
		var workflow = new GitHubActionsWorkflowFile("""
				jobs:
				  analyze:
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: temurin
				        # renovate: datasource=java-version depName=java
				        java-version: 26.0.1
				""");

		workflow.pinTemurinJavaVersion("1.2.3");

		assertThat(workflow.asString()).contains("java-version: 1.2.3");
	}

	@Test
	public void pinTemurinJavaVersionIsIdempotent() {
		var workflow = new GitHubActionsWorkflowFile("""
				jobs:
				  analyze:
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: temurin
				        java-version-file: .tool-versions
				""");

		workflow.pinTemurinJavaVersion("1.2.3");
		String once = workflow.asString();
		workflow.pinTemurinJavaVersion("1.2.3");

		assertThat(workflow.asString()).isEqualTo(once);
	}

	@Test
	public void useToolVersionsFileRemovesThePinAndItsComment() {
		var workflow = new GitHubActionsWorkflowFile("""
				jobs:
				  analyze:
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: temurin
				        # renovate: datasource=java-version depName=java
				        java-version: 1.2.3
				""");

		workflow.useToolVersionsFile();

		assertThat(workflow.asString()).isEqualTo("""
				jobs:
				  analyze:
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: temurin
				        java-version-file: .tool-versions
				""");
	}

	@Test
	public void useToolVersionsFileIsIdempotent() {
		String input = """
				jobs:
				  analyze:
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: temurin
				        java-version-file: .tool-versions
				""";
		var workflow = new GitHubActionsWorkflowFile(input);

		workflow.useToolVersionsFile();

		assertThat(workflow.asString()).isEqualTo(input);
	}

	@Test
	public void grantJobPermissionsKeepsWhatTheJobAlreadyHas() {
		var workflow = new GitHubActionsWorkflowFile("""
				jobs:
				  release:
				    permissions:
				      attestations: write
				      contents: write
				      id-token: write
				    steps:
				    - run: echo
				""");

		workflow.grantJobPermissions("release", Map.of("contents", "write"));

		assertThat(workflow.asString()).isEqualTo("""
				jobs:
				  release:
				    permissions:
				      attestations: write
				      contents: write
				      id-token: write
				    steps:
				    - run: echo
				""");
	}

	@Test
	public void grantJobPermissionsAddsWhatIsMissing() {
		var workflow = new GitHubActionsWorkflowFile("""
				jobs:
				  release:
				    permissions:
				      attestations: write
				    steps:
				    - run: echo
				""");

		workflow.grantJobPermissions("release", Map.of("contents", "write"));

		assertThat(workflow.asString())
				.contains("attestations: write", "contents: write");
	}

}
