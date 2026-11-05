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
	public void commentAboveAStepInAnIndentedSequenceStaysAboveIt() {
		String content = """
				jobs:
				  check:
				    steps:
				      - name: one
				        run: echo one
				      # explains step two
				      - name: two
				        run: echo two
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo("""
						jobs:
						  check:
						    steps:
						    - name: one
						      run: echo one
						    # explains step two
						    - name: two
						      run: echo two
						""");
	}

	@Test
	public void commentAboveACronInAnIndentedSequenceStaysAboveIt() {
		String content = """
				on:
				  schedule:
				    # runs after the nightly backup
				    - cron: "15 5 * * *"
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo("""
						on:
						  schedule:
						  # runs after the nightly backup
						  - cron: "15 5 * * *"
						""");
	}

	@Test
	public void commentGluedToAnIndentedIndicatorStaysGlued() {
		String content = """
				jobs:
				  build:
				    steps:
				      - # this one was written behind the indicator
				        name: one
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo("""
						jobs:
						  build:
						    steps:
						    - # this one was written behind the indicator
						      name: one
						""");
	}

	@Test
	public void blankLineBetweenABlockScalarAndACommentIsKept() {
		String content = """
				jobs:
				  check:
				    steps:
				    - name: one
				      run: |
				        echo one

				    # explains step two
				    - name: two
				      run: |
				        echo two

				      # about uses
				      uses: x
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo(content);
	}

	@Test
	public void blankLineBetweenABlockScalarAndACommentIsKeptWhenIndented() {
		String content = """
				on:
				  schedule:
				    # runs after the nightly backup
				    - cron: "15 5 * * *"

				jobs:
				  check:
				    runs-on: ubuntu-slim
				    steps:
				      - name: one
				        run: |
				          echo one

				      # explains step two
				      - name: two
				        run: |
				          echo two
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo("""
						on:
						  schedule:
						  # runs after the nightly backup
						  - cron: "15 5 * * *"

						jobs:
						  check:
						    runs-on: ubuntu-slim
						    steps:
						    - name: one
						      run: |
						        echo one

						    # explains step two
						    - name: two
						      run: |
						        echo two
						""");
	}

	@Test
	public void literalBlockWithATabStaysALiteralBlock() {
		String content = """
				jobs:
				  build:
				    steps:
				    - name: tab
				      run: |
				        echo "${LINE%%\t*}"
				        printf 'a\tb\\n'
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo(content);
	}

	@Test
	public void literalBlockWithATrailingSpaceStaysALiteralBlock() {
		String content = """
				jobs:
				  build:
				    steps:
				    - name: trailing space
				      run: |
				        echo one\s
				        echo two
				""";

		assertThat(new GitHubActionsWorkflowFile(content).asString())
				.isEqualTo(content);
	}

	@Test
	public void literalBlockKeepsItsContentWhenItHasToBeForced() {
		String content = """
				jobs:
				  build:
				    steps:
				    - run: |-
				        \tindented by a tab
				        spaces then a break \s
				        last line ends in a space\s
				""";
		var workflow = new GitHubActionsWorkflowFile(content);

		var reread = new GitHubActionsWorkflowFile(workflow.asString());

		assertThat(reread.asString()).isEqualTo(workflow.asString());
		assertThat(
				Yamls.scalarValue(
						Yamls.getYamlPath(
								Yamls.load(workflow.asString()).orElseThrow(),
								"/jobs/build/steps/0/run"
						).getFirst()
				)
		).contains(
				"\tindented by a tab\nspaces then a break  \nlast line ends in a space "
		);
	}

	@Test
	public void literalBlockIsNotForcedWhenTheDocumentHoldsTheStandIns() {
		String content = """
				jobs:
				  build:
				    steps:
				    - name: "\uE009"
				      run: |
				        echo "${LINE%%\t*}"
				""";
		var workflow = new GitHubActionsWorkflowFile(content);

		var reread = new GitHubActionsWorkflowFile(workflow.asString());

		assertThat(
				Yamls.scalarValue(
						Yamls.getYamlPath(
								Yamls.load(reread.asString()).orElseThrow(),
								"/jobs/build/steps/0/name"
						).getFirst()
				)
		).contains("\uE009");
	}

	@Test
	public void anchoredSequenceStaysAnchored() {
		String content = """
				on:
				  push:
				    paths: &paths
				    - "**.py"
				    # ruff also formats the Python blocks in Markdown.
				    - "**.md"
				    - ruff.toml
				  pull_request:
				    paths: *paths
				""";
		var workflow = new GitHubActionsWorkflowFile(content);

		workflow.singleToDoubleQuote();

		assertThat(workflow.asString()).isEqualTo(content);
	}

	@Test
	public void singleToDoubleQuoteKeepsComments() {
		String content = """
				jobs:
				  build:
				    steps:
				    # heading
				    - name: 'one' # trailing
				      run: echo one
				""";
		var workflow = new GitHubActionsWorkflowFile(content);

		workflow.singleToDoubleQuote();

		assertThat(workflow.asString()).isEqualTo("""
				jobs:
				  build:
				    steps:
				    # heading
				    - name: one # trailing
				      run: echo one
				""");
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

	private static final String MISE_WORKFLOW = """
			jobs:
			  linux:
			    steps:
			    - uses: actions/setup-java@abc # v6.0.0
			      env:
			        JAVA_VERSION: 1.2.3
			      with:
			        cache: maven
			        distribution: temurin
			        java-version: ${{ env.JAVA_VERSION }}
			    - run: ./mvnw verify
			  native:
			    steps:
			    - uses: actions/setup-java@abc # v6.0.0
			      with:
			        distribution: graalvm
			        java-version-file: .tool-versions
			    - run: ./mvnw verify
			  other:
			    steps:
			    - uses: actions/setup-java@abc # v6.0.0
			      with:
			        java-version-file: src/.tool-versions
			  helpers:
			    steps:
			    - uses: jdx/mise-action@m # v4
			    - uses: actions/cache@c
			""";

	private static GitHubActionsWorkflowFile miseSource() {
		return new GitHubActionsWorkflowFile(MISE_WORKFLOW);
	}

	private static GitHubActionsWorkflowFile withMise(String workflow) {
		var file = new GitHubActionsWorkflowFile(workflow);
		var source = miseSource();
		file.replaceSetupJavaWithMise(
				source.getStepUsing("helpers", "jdx/mise-action").orElseThrow(),
				source.getStepUsing("helpers", "actions/cache").orElseThrow()
		);
		return file;
	}

	@Test
	public void replaceSetupJavaWithMiseReplacesTemurinAndGraal() {
		var output = withMise(MISE_WORKFLOW).asString();

		assertThat(output).doesNotContain("JAVA_VERSION")
				.doesNotContain("distribution: temurin")
				.doesNotContain("distribution: graalvm");
		var file = new GitHubActionsWorkflowFile(output);
		assertThat(file.hasStepUsing("linux", "jdx/mise-action")).isTrue();
		assertThat(file.hasStepUsing("native", "jdx/mise-action")).isTrue();
		assertThat(output).doesNotContainPattern("[&*]id\\d+");
	}

	@Test
	public void replaceSetupJavaWithMiseAddsCacheOnlyForMaven() {
		var file = withMise(MISE_WORKFLOW);

		assertThat(file.hasStepUsing("linux", "actions/cache")).isTrue();
		assertThat(file.hasStepUsing("native", "actions/cache")).isFalse();
	}

	@Test
	public void replaceSetupJavaWithMiseKeepsOtherVersionFiles() {
		var file = withMise(MISE_WORKFLOW);

		assertThat(file.hasStepUsing("other", "actions/setup-java")).isTrue();
		assertThat(file.hasStepUsing("other", "jdx/mise-action")).isFalse();
	}

	@Test
	public void replaceSetupJavaWithMiseIsIdempotent() {
		var once = withMise(MISE_WORKFLOW);
		var source = miseSource();

		once.replaceSetupJavaWithMise(
				source.getStepUsing("helpers", "jdx/mise-action").orElseThrow(),
				source.getStepUsing("helpers", "actions/cache").orElseThrow()
		);

		assertThat(once.asString())
				.isEqualTo(withMise(MISE_WORKFLOW).asString());
	}

	private static final String MAVEN_STEPS = """
			jobs:
			  build:
			    steps:
			    - run: ./mvnw verify
			      env:
			        GITHUB_TOKEN: x
			    - uses: github/codeql-action/autobuild@x
			    - run: echo hi
			""";

	@Test
	public void setEnvOnMavenStepsTargetsMvnwAndAutobuild() {
		var file = new GitHubActionsWorkflowFile(MAVEN_STEPS);

		file.setEnvOnMavenSteps(
				"build",
				"SKIP_GRAALVM_NATIVE_IMAGE_BUILD",
				"true"
		);

		assertThat(file.asString()).isEqualTo("""
				jobs:
				  build:
				    steps:
				    - run: ./mvnw verify
				      env:
				        GITHUB_TOKEN: x
				        SKIP_GRAALVM_NATIVE_IMAGE_BUILD: true
				    - uses: github/codeql-action/autobuild@x
				      env:
				        SKIP_GRAALVM_NATIVE_IMAGE_BUILD: true
				    - run: echo hi
				""");
	}

	@Test
	public void setEnvOnMavenStepsKeepsExistingEnv() {
		var file = new GitHubActionsWorkflowFile(MAVEN_STEPS);

		file.setEnvOnMavenSteps(
				"build",
				"SKIP_GRAALVM_NATIVE_IMAGE_BUILD",
				"true"
		);
		var once = file.asString();
		file.setEnvOnMavenSteps(
				"build",
				"SKIP_GRAALVM_NATIVE_IMAGE_BUILD",
				"true"
		);

		assertThat(once).contains("GITHUB_TOKEN: x");
		assertThat(file.asString()).isEqualTo(once);
	}

	@Test
	public void setEnvOnMavenStepsLeavesUnquotedBooleanUnchanged() {
		var input = """
				jobs:
				  build:
				    steps:
				    - run: ./mvnw verify
				      env:
				        SKIP_GRAALVM_NATIVE_IMAGE_BUILD: true
				""";
		var file = new GitHubActionsWorkflowFile(input);

		file.setEnvOnMavenSteps(
				"build",
				"SKIP_GRAALVM_NATIVE_IMAGE_BUILD",
				"true"
		);

		assertThat(file.asString()).isEqualTo(input);
	}

	@Test
	public void removeEnvFromMavenStepsDropsEmptyEnv() {
		var file = new GitHubActionsWorkflowFile(MAVEN_STEPS);
		file.setEnvOnMavenSteps(
				"build",
				"SKIP_GRAALVM_NATIVE_IMAGE_BUILD",
				"true"
		);

		file.removeEnvFromMavenSteps(
				"build",
				"SKIP_GRAALVM_NATIVE_IMAGE_BUILD"
		);

		assertThat(file.asString()).isEqualTo(
				new GitHubActionsWorkflowFile(MAVEN_STEPS).asString()
		);
		assertThat(file.asString()).contains("GITHUB_TOKEN: x");
		assertThat(
				file.getStepUsing("build", "github/codeql-action/autobuild")
						.map(n -> n.toString())
						.orElse("")
		).doesNotContain("env");
	}

}
