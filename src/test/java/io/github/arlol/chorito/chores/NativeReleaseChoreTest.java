package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class NativeReleaseChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	/**
	 * The shape every chorito-managed native CLI was on before this chore. It
	 * does not mention GraalVM: only .tool-versions says what it builds on.
	 */
	private static final String NATIVE_CLI = """
			jobs:
			  linux:
			    runs-on: ubuntu-latest
			    steps:
			    - uses: jdx/mise-action@v5
			  windows:
			    runs-on: windows-latest
			    steps:
			    - uses: jdx/mise-action@v5
			  release:
			    needs:
			    - version
			    - macos
			    - linux
			    - windows
			    runs-on: ubuntu-latest
			    steps:
			    - name: Download all workflow run artifacts
			      uses: actions/download-artifact@v8
			      with:
			        path: ./target
			    - name: Prepare artifacts
			      run: |
			        mv "${ARTIFACT}-linux-${NEW_VERSION}/x" "artifacts/${ARTIFACT}-linux"
			    - name: Create Release
			      run: gh release create "v1" ./target/artifacts/*
			  required-status-check:
			    needs:
			    - macos
			    - linux
			    - windows
			    runs-on: ubuntu-latest
			    steps:
			    - run: echo hi
			""";

	/** A library proving itself inside a native image: nothing to download. */
	private static final String GRAAL_LIBRARY = """
			jobs:
			  version:
			    runs-on: ubuntu-latest
			    steps:
			    - run: echo version
			  linux:
			    runs-on: ubuntu-latest
			    steps:
			    - uses: jdx/mise-action@v5
			  windows:
			    runs-on: windows-latest
			    steps:
			    - uses: jdx/mise-action@v5
			  release:
			    needs:
			    - version
			    - linux
			    - windows
			    runs-on: ubuntu-latest
			    steps:
			    - name: Create Release
			      run: gh release create "v1"
			  required-status-check:
			    needs:
			    - linux
			    - windows
			    runs-on: ubuntu-latest
			    steps:
			    - run: echo hi
			""";

	private void writeToolVersions(String content) {
		FilesSilent.writeString(
				extension.root().resolve(".tool-versions"),
				content
		);
	}

	private Path writeMain(String content) {
		writeToolVersions("java graalvm-community-25.0.2\n");
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, content);
		return workflow;
	}

	@Test
	public void testAddsTheArmJobAfterLinux() {
		Path workflow = writeMain(NATIVE_CLI);

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.contains("  linux-arm:")
				.containsPattern("runs-on: ubuntu-[0-9.]+-arm");
		assertThat(FilesSilent.readString(workflow).indexOf("  linux-arm:"))
				.isGreaterThan(
						FilesSilent.readString(workflow).indexOf("  linux:")
				)
				.isLessThan(
						FilesSilent.readString(workflow).indexOf("  windows:")
				);
	}

	@Test
	public void testInnovationPinGetsReleaseLayout() {
		Path workflow = writeMain(NATIVE_CLI);
		writeToolVersions("java graalvm-community-innovation-25.0.5.0.3\n");

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.contains("  linux-arm:")
				.contains("${ARTIFACT}-${os}-${arch}.tar.gz");
	}

	@Test
	public void testDetectsNativeCliFromToolVersions() {
		assertThat(NATIVE_CLI).doesNotContain("graalvm");
		Path workflow = writeMain(NATIVE_CLI);

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content().contains("  linux-arm:");
	}

	@Test
	public void testWaitsForTheArmBuildBeforeReleasing() {
		Path workflow = writeMain(NATIVE_CLI);

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content().contains("""
				  release:
				    needs:
				    - version
				    - macos
				    - linux
				    - linux-arm
				    - windows
				""").contains("""
				  required-status-check:
				    needs:
				    - macos
				    - linux
				    - linux-arm
				    - windows
				""");
	}

	@Test
	public void testNamesEachAssetAfterItsOsAndArch() {
		Path workflow = writeMain(NATIVE_CLI);

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.contains("${ARTIFACT}-${os}-${arch}.tar.gz")
				.contains("${ARTIFACT}-windows-x64.zip")
				.doesNotContain("\"artifacts/${ARTIFACT}-linux\"");
	}

	@Test
	public void testLeavesAWorkflowThatBuildsNoNativeImage() {
		Path workflow = writeMain(NATIVE_CLI);
		writeToolVersions("java temurin-25.0.5+8.0.LTS\n");

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(NATIVE_CLI);
	}

	@Test
	public void testLeavesAReleaseThatCollectsNoBinaries() {
		String noDownload = NATIVE_CLI
				.replace("      uses: actions/download-artifact@v8\n", "");
		Path workflow = writeMain(noDownload);

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.contains("  linux-arm:")
				.contains("\"artifacts/${ARTIFACT}-linux\"")
				.doesNotContain("${ARTIFACT}-${os}-${arch}.tar.gz");
	}

	@Test
	public void testIsIdempotent() {
		Path workflow = writeMain(NATIVE_CLI);
		new NativeReleaseChore().doit(extension.choreContext());
		String once = FilesSilent.readString(workflow);

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(once);
	}

	@Test
	public void testLibraryGetsAnArmJobWithoutUpload() {
		Path workflow = writeMain(GRAAL_LIBRARY);

		new NativeReleaseChore().doit(extension.choreContext());

		String content = FilesSilent.readString(workflow);
		int arm = content.indexOf("  linux-arm:");
		int windows = content.indexOf("  windows:");
		assertThat(arm).isGreaterThan(content.indexOf("  linux:"))
				.isLessThan(windows);
		assertThat(content.substring(arm, windows)).contains("-P graal")
				.doesNotContain("upload-artifact")
				.doesNotContain("Move artifacts");
		assertThat(content).contains("""
				  release:
				    needs:
				    - version
				    - linux
				    - linux-arm
				    - windows
				    runs-on: ubuntu-latest
				    steps:
				    - name: Create Release
				      run: gh release create "v1"
				""").contains("""
				  required-status-check:
				    needs:
				    - linux
				    - linux-arm
				    - windows
				""");
	}

	@Test
	public void testLibraryIsIdempotent() {
		Path workflow = writeMain(GRAAL_LIBRARY);
		new NativeReleaseChore().doit(extension.choreContext());
		String once = FilesSilent.readString(workflow);

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(once);
	}

	@Test
	public void testLibraryArmJobSurvivesGitHubActionChore() {
		Path workflow = writeMain(GRAAL_LIBRARY);
		new NativeReleaseChore().doit(extension.choreContext());

		new GitHubActionChore().doit(extension.choreContext());
		new NativeReleaseChore().doit(extension.choreContext());
		String once = FilesSilent.readString(workflow);
		new GitHubActionChore().doit(extension.choreContext());
		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.contains("  linux-arm:")
				.doesNotContain("upload-artifact")
				.isEqualTo(once);
	}

	@Test
	public void testLibraryWithArmJobAlreadyThereLosesItsUpload() {
		String withArm = GRAAL_LIBRARY.replace(
				"  windows:",
				"  linux-arm:\n" + "    runs-on: ubuntu-24.04-arm\n"
						+ "    steps:\n"
						+ "    - uses: actions/upload-artifact@v7\n"
						+ "  windows:"
		);
		Path workflow = writeMain(withArm);

		new GitHubActionChore().doit(extension.choreContext());
		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.contains("  linux-arm:")
				.doesNotContain("upload-artifact");
	}

	@Test
	public void testNativeCliKeepsItsUpload() {
		Path workflow = writeMain(NATIVE_CLI);

		new NativeReleaseChore().doit(extension.choreContext());

		String content = FilesSilent.readString(workflow);
		int arm = content.indexOf("  linux-arm:");
		assertThat(content.substring(arm, content.indexOf("  windows:")))
				.contains("upload-artifact");
	}

	@Test
	public void testTemurinRepoGetsNoArmJob() {
		Path workflow = writeMain(GRAAL_LIBRARY);
		writeToolVersions("java temurin-25.0.5+8.0.LTS\n");

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(GRAAL_LIBRARY);
	}

	@Test
	public void testGluonfxRepoGetsNoArmJob() {
		String gluon = GRAAL_LIBRARY.replace("echo hi", "mvn gluonfx:build");
		Path workflow = writeMain(gluon);

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(gluon);
	}

	@Test
	public void testNonMainWorkflowWithReleaseGetsNoArmJob() {
		writeToolVersions("java graalvm-community-25.0.2\n");
		String other = GRAAL_LIBRARY.replace("  linux:", "  build:")
				.replace("    - linux\n", "    - build\n");
		Path workflow = extension.root()
				.resolve(".github/workflows/other.yaml");
		FilesSilent.writeString(workflow, other);

		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(other);
	}

	@Test
	public void testWithNothing() {
		new NativeReleaseChore().doit(extension.choreContext());

		assertThat(extension.relativePaths()).isEmpty();
	}

}
