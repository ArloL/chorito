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
	public void removeVersionsStripsRenovatePinnedToolVersion() {
		assertThat(
				GitHubActionsWorkflowFile.removeVersions(
						"      env:\n        # renovate: datasource=pypi depName=zizmor\n        ZIZMOR_VERSION: 1.30.1\n"
				)
		).isEqualTo(
				"      env:\n        # renovate: datasource=pypi depName=zizmor\n        ZIZMOR_VERSION:\n"
		);
	}

	@Test
	public void removeVersionsKeepsVersionWithoutRenovateComment() {
		String input = "      env:\n        ZIZMOR_VERSION: 1.30.1\n";
		assertThat(GitHubActionsWorkflowFile.removeVersions(input))
				.isEqualTo(input);
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
	public void commentAboveStepStaysAboveStep() {
		String content = """
				jobs:
				  build:
				    steps:
				    # attest before the release exists, because an installer
				    # cannot trust an asset it cannot verify
				    - name: Attest the release assets
				      uses: actions/attest-build-provenance@abc # v4.2.2
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo(content);
	}

	@Test
	public void commentAboveLaterKeyStaysWhereItIs() {
		String content = """
				jobs:
				  build:
				    steps:
				    - name: Attest the release assets
				      # this placement round-trips unchanged
				      uses: actions/attest-build-provenance@abc # v4.2.2
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo(content);
	}

	@Test
	public void commentAboveAnyKindOfSequenceItemStaysAboveIt() {
		String content = """
				on:
				  schedule:
				  # nightly, offset so it does not collide with the hourly runs
				  - cron: "0 3 * * *"
				  push:
				    branches:
				    # only main: a fork's pushes never reach this repository
				    - main
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo(content);
	}

	@Test
	public void commentAfterABlockScalarStaysAboveTheNextItem() {
		String content = """
				jobs:
				  release:
				    steps:
				    - name: one
				      run: |
				        echo one
				    # heading
				    - name: two
				      run: echo two
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo(content);
	}

	@Test
	public void commentAfterABlockScalarStaysAboveALaterKey() {
		String content = """
				jobs:
				  release:
				    steps:
				    - name: one
				      run: |
				        echo one
				      # about uses
				      uses: x
				    - name: two
				      run: echo two
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo(content);
	}

	@Test
	public void aCopiedStepCarriesItsHeadingComment() {
		var source = new GitHubActionsWorkflowFile("""
				jobs:
				  release:
				    steps:
				    - name: one
				      run: |
				        echo one
				    # heading
				    - name: two
				      run: echo two
				""");
		var target = new GitHubActionsWorkflowFile("""
				jobs:
				  release:
				    steps:
				    - name: last
				      run: echo last
				""");

		target.insertStepBefore(
				"release",
				"last",
				source.getStepByName("release", "two").orElseThrow()
		);

		assertThat(target.asString()).contains("# heading");
	}

	@Test
	public void commentGluedToTheIndicatorStaysGlued() {
		String content = """
				jobs:
				  build:
				    steps:
				    - # this one was written behind the indicator
				      name: Attest the release assets
				      uses: actions/attest-build-provenance@abc # v4.2.2
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo(content);
	}

	@Test
	public void commentLikeBlockScalarContentIsLeftAlone() {
		String content = """
				jobs:
				  build:
				    steps:
				    - name: Write the changelog
				      run: |
				        cat <<'MD' > CHANGELOG.md
				        - # heading in a markdown list
				        MD
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo(content);
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

	@Test
	public void removeStepByNameRemovesThatStepOnly() {
		var workflow = new GitHubActionsWorkflowFile("""
				jobs:
				  linux:
				    steps:
				    - name: Build with Maven
				      run: ./mvnw verify
				    - name: Move artifacts
				      run: mkdir target/artifacts
				    - name: Make sure build did not change anything
				      run: git diff --exit-code
				""");

		workflow.removeStepByName("linux", "Move artifacts");

		assertThat(workflow.asString()).isEqualTo("""
				jobs:
				  linux:
				    steps:
				    - name: Build with Maven
				      run: ./mvnw verify
				    - name: Make sure build did not change anything
				      run: git diff --exit-code
				""");
	}

	@Test
	public void removeStepUsingRemovesTheStepWithThatAction() {
		var workflow = new GitHubActionsWorkflowFile("""
				jobs:
				  linux:
				    steps:
				    - uses: actions/checkout@abc # v7.0.1
				    - uses: actions/upload-artifact@def # v7.0.1
				      with:
				        path: target/artifacts
				    - name: Make sure build did not change anything
				      run: git diff --exit-code
				""");

		workflow.removeStepUsing("linux", "actions/upload-artifact");

		assertThat(workflow.asString()).isEqualTo("""
				jobs:
				  linux:
				    steps:
				    - uses: actions/checkout@abc # v7.0.1
				    - name: Make sure build did not change anything
				      run: git diff --exit-code
				""");
	}

	@Test
	public void releasePublishesAssetsReadsTheReleaseJob() {
		assertThat(new GitHubActionsWorkflowFile("""
				jobs:
				  release:
				    steps:
				    - name: Create Release
				      run: gh release create v1 ./target/artifacts/*
				""").releasePublishesAssets()).isTrue();

		assertThat(new GitHubActionsWorkflowFile("""
				jobs:
				  release:
				    steps:
				    - name: Create Release
				      run: gh release create v1
				""").releasePublishesAssets()).isFalse();

		assertThat(new GitHubActionsWorkflowFile("""
				jobs:
				  linux:
				    steps:
				    - run: echo
				""").releasePublishesAssets()).isFalse();
	}

}
