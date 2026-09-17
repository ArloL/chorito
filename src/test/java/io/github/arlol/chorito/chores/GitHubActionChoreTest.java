package io.github.arlol.chorito.chores;

import static io.github.arlol.chorito.tools.GitHubActionsWorkflowFile.removeVersions;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FakeRandomGenerator;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JavaVersions;

public class GitHubActionChoreTest {

	private static final String INPUT_ADOPT = """
			jobs:
			  linux:
			    runs-on: ubuntu-latest
			    steps:
			    - uses: actions/setup-java@v3.5.1
			      with:
			        distribution: adopt
			""";
	private static final String EXPECTED_ADOPT = """
			permissions: {}
			jobs:
			  linux:
			    runs-on: ubuntu-latest
			    steps:
			    - uses: actions/setup-java@v3.5.1
			      with:
			        distribution: temurin
			        java-version-file: .tool-versions
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new GitHubActionChore().doit(extension.choreContext());

		assertThat(extension.relativePaths()).isEmpty();
	}

	@Test
	public void testEmptyWorkflowFile() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.touch(workflow);

		new GitHubActionChore().doit(extension.choreContext());

		assertThat(workflow).content().isEmpty();
	}

	@Test
	public void testBasicWorkflowFile() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");

		FilesSilent.writeString(workflow, """
				jobs:
				  linux:
				    runs-on: ubuntu-latest
				    steps:
				    - run: whoami
				""");

		new GitHubActionChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo("""
				permissions: {}
				jobs:
				  linux:
				    runs-on: ubuntu-latest
				    steps:
				    - run: whoami
				""");
	}

	@Test
	public void testReusableWorkflowJobKeepsNoSteps() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");

		FilesSilent.writeString(workflow, """
				jobs:
				  call:
				    uses: ./.github/workflows/reusable.yaml
				""");

		new GitHubActionChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo("""
				permissions: {}
				jobs:
				  call:
				    uses: ./.github/workflows/reusable.yaml
				""");
	}

	@Test
	public void testAdoptTemurinMigration() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, INPUT_ADOPT);

		new GitHubActionChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(EXPECTED_ADOPT);
	}

	@Test
	public void testChoresSetSchedule53445() throws Exception {
		Path workflow = extension.root()
				.resolve(".github/workflows/chores.yaml");
		FilesSilent.writeString(workflow, """
				on:
				  schedule:
				  - cron: '5 3 4 4 *'
				""");

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.build();

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().startsWith("""
				name: Chores

				on:
				  workflow_dispatch:
				  repository_dispatch:
				    types:
				    - chores
				  schedule:
				  - cron: "5 3 4 4 *"
				""");
	}

	@Test
	public void testChoresSetSchedule26155() throws Exception {
		Path workflow = extension.root()
				.resolve(".github/workflows/chores.yaml");
		FilesSilent.writeString(workflow, """
				on:
				  schedule:
				  - cron: '26 15 * * 5'
				""");

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.build();

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().doesNotStartWith("""
				name: Chores

				on:
				  workflow_dispatch:
				  repository_dispatch:
				    types:
				    - chores
				  schedule:
				  - cron: '26 15 * * 5'
				""");
	}

	@Test
	public void testCodeQlSetSchedule4203() throws Exception {
		Path workflow = extension.root()
				.resolve(".github/workflows/codeql-analysis.yaml");
		String input = """
				on:
				  schedule:
				    - cron: '4 20 * * 3'
				""";
		FilesSilent.writeString(workflow, input);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().isEqualTo("""
				on:
				  schedule:
				  - cron: "1 3 1 * *"
				permissions: {}
				""");
	}

	@Test
	public void testMainSchedulDuplicated() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		String input = """
				on:
				  schedule:
				  - cron: '17 4 5 * *'
				""";
		FilesSilent.writeString(workflow, input);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().isEqualTo("""
				on:
				  schedule:
				  - cron: "1 3 1 * *"
				permissions: {}
				""");
	}

	@Test
	public void testMainScheduleAlreadyReplaced() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		String input = """
				on:
				  schedule:
				  - cron: '5 5 5 * *'
				""";
		FilesSilent.writeString(workflow, input);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().isEqualTo("""
				on:
				  schedule:
				  - cron: "5 5 5 * *"
				permissions: {}
				""");
	}

	@Test
	public void testUpxRemoval() {
		Path pom = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				pom,
				ClassPathFiles
						.readString("github-actions/upx-removal-input.yaml")
		);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().updateGraalSteps(context);

		String expected = ClassPathFiles
				.readString("github-actions/upx-removal-output.yaml");
		assertThat(removeVersions(FilesSilent.readString(pom)))
				.isEqualTo(removeVersions(expected));
	}

	@Test
	public void testNoEmptyLines() {
		Path pom = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				pom,
				ClassPathFiles
						.readString("github-actions/no-empty-lines-input.yaml")
		);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.branches(List.of("main"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		String expected = ClassPathFiles
				.readString("github-actions/no-empty-lines-output.yaml");
		assertThat(pom).content().isEqualTo(expected);
	}

	@Test
	public void testPermissionUpdate() {
		Path pom = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				pom,
				ClassPathFiles
						.readString("github-actions/permission-input.yaml")
		);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().updatePermissions(context);

		String expected = ClassPathFiles
				.readString("github-actions/permission-expected.yaml");
		assertThat(pom).content().isEqualTo(expected);
	}

	@Test
	public void testCodeQlRemovalOfSpecificVersions() {
		Path workflow = extension.root()
				.resolve(".github/workflows/codeql-analysis.yaml");
		FilesSilent.writeString(workflow, """
				jobs:
				  analyze:
				    steps:
				    - uses: github/codeql-action/init@v3.24.4
				    - uses: github/codeql-action/autobuild@v3.24.5
				    - uses: github/codeql-action/analyze@v3.25.6
				""");

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().isEqualTo("""
				permissions: {}
				jobs:
				  analyze:
				    steps:
				    - uses: github/codeql-action/init@v3
				    - uses: github/codeql-action/autobuild@v3
				    - uses: github/codeql-action/analyze@v3
				""");
	}

	@Test
	void choresWorkflowShouldNotUpdateVersions() throws Exception {
		String outdatedChoresWorkflow = ClassPathFiles
				.readString("github-actions/outdated-chores-workflow.yaml");
		Path workflow = extension.root()
				.resolve(".github/workflows/chores.yaml");
		FilesSilent.writeString(workflow, outdatedChoresWorkflow);
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();
		new GitHubActionChore().doit(context);
		assertThat(workflow).content().isEqualTo(outdatedChoresWorkflow);
	}

	@Test
	void shouldNotUpdateVersions() throws Exception {
		String outdatedWorkflow = ClassPathFiles
				.readString("github-actions/outdated-main-workflow.yaml");
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, outdatedWorkflow);
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.branches(List.of("main"))
				.randomGenerator(new FakeRandomGenerator())
				.build();
		new GitHubActionChore().doit(context);
		assertThat(workflow).content().isEqualTo(outdatedWorkflow);
	}

	@Test
	void shouldCreateCheckActionsWorkflow() throws Exception {
		Path workflow = extension.root()
				.resolve(".github/workflows/check-actions.yaml");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();
		new GitHubActionChore().doit(context);
		assertThat(workflow).content().contains("1 3 1 * *");
	}

	@Test
	void checkActionsWorkflowShouldNotUpdateToolVersions() throws Exception {
		Path workflow = extension.root()
				.resolve(".github/workflows/check-actions.yaml");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();
		new GitHubActionChore().doit(context);
		String bumped = FilesSilent.readString(workflow)
				.replaceFirst("ZIZMOR_VERSION: \\S+", "ZIZMOR_VERSION: 99.0.0");
		FilesSilent.writeString(workflow, bumped);

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().isEqualTo(bumped);
	}

	/**
	 * check-actions.yaml is shipped as written, and it says main because
	 * chorito does. A repository on master needs the name swapped or it
	 * receives a workflow whose triggers match nothing, which is not a failure
	 * anybody sees -- the workflow simply never runs.
	 */
	@Test
	void shouldCreateCheckActionsWorkflowForAMasterRepository()
			throws Exception {
		Path workflow = extension.root()
				.resolve(".github/workflows/check-actions.yaml");
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.branches(List.of("master"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().contains("""
				  push:
				    branches:
				    - master
				  pull_request:
				    branches:
				    - master
				""").doesNotContain("- main");
	}

	@Test
	void actionsCheckoutWithPersistCredentials() throws Exception {
		String input = ClassPathFiles
				.readString("github-actions/actions-checkout-input.yaml");
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, input);
		new GitHubActionChore().doit(extension.choreContext());
		String expected = ClassPathFiles
				.readString("github-actions/actions-checkout-output.yaml");
		assertThat(workflow).content().isEqualTo(expected);
	}

	@Test
	void actionsCheckoutWithPersistCredentialsInYmlWorkflow() throws Exception {
		String input = ClassPathFiles
				.readString("github-actions/actions-checkout-input.yaml");
		Path workflow = extension.root().resolve(".github/workflows/ci.yml");
		FilesSilent.writeString(workflow, input);
		new GitHubActionChore().doit(extension.choreContext());
		String expected = ClassPathFiles
				.readString("github-actions/actions-checkout-output.yaml");
		assertThat(workflow).content().isEqualTo(expected);
	}

	@Test
	public void testBasicWorkflowFileWithYmlExtension() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yml");

		FilesSilent.writeString(workflow, """
				jobs:
				  linux:
				    runs-on: ubuntu-latest
				    steps:
				    - run: whoami
				""");

		new GitHubActionChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo("""
				permissions: {}
				jobs:
				  linux:
				    runs-on: ubuntu-latest
				    steps:
				    - run: whoami
				""");
	}

	@Test
	void quotedRedirects() throws Exception {
		String input = ClassPathFiles
				.readString("github-actions/quoted-redirects-input.yaml");
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, input);
		new GitHubActionChore().doit(extension.choreContext());
		String expected = ClassPathFiles
				.readString("github-actions/quoted-redirects-output.yaml");
		assertThat(workflow).content().isEqualTo(expected);
	}

	@Test
	void migrateNcipoploReleaseAction() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				workflow,
				"""
						permissions: {}
						jobs:
						  release:
						    runs-on: ubuntu-latest
						    permissions:
						      contents: write
						    steps:
						    - name: Create Release
						      id: create_release
						      uses: ncipollo/release-action@b7eabc95ff50cbeeedec83973935c8f306dfcd0b # v1.20.0
						      with:
						        draft: true
						        name: Release ${{ needs.version.outputs.new_version }}
						        tag: v${{ needs.version.outputs.new_version }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@8f6863c6c894ba46f9e676ef5cccec4752723c1e # v1.9.2
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-linux-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/linux.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@8f6863c6c894ba46f9e676ef5cccec4752723c1e # v1.9.2
						      with:
						        asset_content_type: application/x-executable
						        asset_name: ${{ env.ARTIFACT }}-linux
						        asset_path: ./target/${{ env.ARTIFACT }}-linux-${{ needs.version.outputs.new_version }}/${{ env.ARTIFACT }}-linux-${{ needs.version.outputs.new_version }}
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@8f6863c6c894ba46f9e676ef5cccec4752723c1e # v1.9.2
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-windows-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/windows.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@8f6863c6c894ba46f9e676ef5cccec4752723c1e # v1.9.2
						      with:
						        asset_content_type: application/vnd.microsoft.portable-executable
						        asset_name: ${{ env.ARTIFACT }}-windows.exe
						        asset_path: ./target/${{ env.ARTIFACT }}-windows-${{ needs.version.outputs.new_version }}/${{ env.ARTIFACT }}-windows-${{ needs.version.outputs.new_version }}.exe
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@8f6863c6c894ba46f9e676ef5cccec4752723c1e # v1.9.2
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-macos-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/macos.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@8f6863c6c894ba46f9e676ef5cccec4752723c1e # v1.9.2
						      with:
						        asset_content_type: application/octet-stream
						        asset_name: ${{ env.ARTIFACT }}-macos
						        asset_path: ./target/${{ env.ARTIFACT }}-macos-${{ needs.version.outputs.new_version }}/${{ env.ARTIFACT }}-macos-${{ needs.version.outputs.new_version }}
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - uses: ncipollo/release-action@b7eabc95ff50cbeeedec83973935c8f306dfcd0b # v1.20.0
						      with:
						        allowUpdates: true
						        immutableCreate: true
						        omitBodyDuringUpdate: true
						        omitNameDuringUpdate: true
						        tag: v${{ needs.version.outputs.new_version }}
						        updateOnlyUnreleased: true
						"""
		);
		new GitHubActionChore().doit(extension.choreContext());
		assertThat(workflow).content()
				.contains("gh release create")
				.doesNotContain("ncipollo/release-action")
				.doesNotContain("shogo82148/actions-upload-release-asset");
	}

	@Test
	void migrateNcipoploReleaseActionZipOnly() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				workflow,
				"""
						permissions: {}
						jobs:
						  release:
						    runs-on: ubuntu-latest
						    permissions:
						      contents: write
						    steps:
						    - name: Create Release
						      id: create_release
						      uses: ncipollo/release-action@339a81892b84b4eeb0f6e744e4574d79d0d9b8dd # v1.21.0
						      with:
						        draft: true
						        name: Release ${{ needs.version.outputs.new_version }}
						        tag: v${{ needs.version.outputs.new_version }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@96bc1f0cb850b65efd58a6b5eaa0a69f88d38077 # v1.10.0
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-linux-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/linux.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@96bc1f0cb850b65efd58a6b5eaa0a69f88d38077 # v1.10.0
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-windows-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/windows.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@96bc1f0cb850b65efd58a6b5eaa0a69f88d38077 # v1.10.0
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-macos-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/macos.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - uses: ncipollo/release-action@339a81892b84b4eeb0f6e744e4574d79d0d9b8dd # v1.21.0
						      with:
						        allowUpdates: true
						        immutableCreate: true
						        omitBodyDuringUpdate: true
						        omitNameDuringUpdate: true
						        tag: v${{ needs.version.outputs.new_version }}
						        updateOnlyUnreleased: true
						"""
		);
		new GitHubActionChore().doit(extension.choreContext());
		assertThat(workflow).content()
				.contains("gh release create")
				.doesNotContain("ncipollo/release-action")
				.doesNotContain("shogo82148/actions-upload-release-asset");
	}

	@Test
	public void testGraalProjectPinsTemurinSteps() throws Exception {
		FilesSilent.writeString(
				extension.root().resolve(".tool-versions"),
				"java graalvm-community-25.0.2\n"
		);
		Path workflow = extension.root()
				.resolve(".github/workflows/sonarcloud.yaml");
		FilesSilent.writeString(workflow, """
				jobs:
				  sonarcloud:
				    runs-on: ubuntu-latest
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: temurin
				        java-version-file: .tool-versions
				""");

		new GitHubActionChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.isEqualTo(
						"""
								permissions: {}
								jobs:
								  sonarcloud:
								    runs-on: ubuntu-latest
								    steps:
								    - uses: actions/setup-java@abc # v6.0.0
								      env:
								        # renovate: datasource=java-version depName=java extractVersion=^(?<version>\\d+\\.\\d+\\.\\d+)
								        JAVA_VERSION: $TEMURIN
								      with:
								        cache: maven
								        distribution: temurin
								        java-version: ${{ env.JAVA_VERSION }}
								"""
								.replace("$TEMURIN", JavaVersions.TEMURIN)
				);
	}

	@Test
	public void testGraalProjectLeavesNativeImageStepsOnToolVersions()
			throws Exception {
		FilesSilent.writeString(
				extension.root().resolve(".tool-versions"),
				"java graalvm-community-25.0.2\n"
		);
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, """
				jobs:
				  linux:
				    runs-on: ubuntu-latest
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: graalvm
				        java-version-file: .tool-versions
				""");

		new GitHubActionChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo("""
				permissions: {}
				jobs:
				  linux:
				    runs-on: ubuntu-latest
				    steps:
				    - uses: actions/setup-java@abc # v6.0.0
				      with:
				        cache: maven
				        distribution: graalvm
				        java-version-file: .tool-versions
				""");
	}

	private static final String GRAAL_MAIN_WITHOUT_RELEASE_ASSETS = """
			jobs:
			  version:
			    runs-on: ubuntu-latest
			    steps:
			    - run: echo version
			  linux:
			    needs: version
			    runs-on: ubuntu-latest
			    steps:
			    - uses: actions/setup-java@abc # v6.0.0
			      with:
			        distribution: graalvm
			        java-version-file: .tool-versions
			  release:
			    runs-on: ubuntu-latest
			    steps:
			    - name: Create Release
			      run: gh release create "v1"
			""";

	@Test
	public void testKeepsUploadsOutOfAReleaseThatDownloadsNothing()
			throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, GRAAL_MAIN_WITHOUT_RELEASE_ASSETS);

		new GitHubActionChore().updateGraalSteps(extension.choreContext());

		assertThat(workflow).content()
				.doesNotContain("upload-artifact")
				.doesNotContain("Move artifacts");
	}

	@Test
	public void testKeepsUploadsWhenTheReleaseDownloadsThem() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				workflow,
				GRAAL_MAIN_WITHOUT_RELEASE_ASSETS.replace(
						"    - name: Create Release",
						"    - uses: actions/download-artifact@abc # v8.0.1\n"
								+ "    - name: Create Release"
				)
		);

		new GitHubActionChore().updateGraalSteps(extension.choreContext());

		assertThat(workflow).content()
				.contains("upload-artifact")
				.contains("Move artifacts");
	}

}
