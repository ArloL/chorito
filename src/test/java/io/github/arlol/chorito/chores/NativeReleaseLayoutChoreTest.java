package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class NativeReleaseLayoutChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	/** The shape every chorito-managed native CLI was on before this chore. */
	private static final String NATIVE_CLI = """
			jobs:
			  linux:
			    runs-on: ubuntu-latest
			    steps:
			    - uses: actions/setup-java@v6
			      with:
			        distribution: graalvm
			  windows:
			    runs-on: windows-latest
			    steps:
			    - uses: actions/setup-java@v6
			      with:
			        distribution: graalvm
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

	private Path writeMain(String content) {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, content);
		return workflow;
	}

	@Test
	public void testAddsTheArmJobAfterLinux() {
		Path workflow = writeMain(NATIVE_CLI);

		new NativeReleaseLayoutChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.contains("  linux-arm:")
				.contains("runs-on: ubuntu-24.04-arm");
		assertThat(FilesSilent.readString(workflow).indexOf("  linux-arm:"))
				.isGreaterThan(
						FilesSilent.readString(workflow).indexOf("  linux:")
				)
				.isLessThan(
						FilesSilent.readString(workflow).indexOf("  windows:")
				);
	}

	@Test
	public void testWaitsForTheArmBuildBeforeReleasing() {
		Path workflow = writeMain(NATIVE_CLI);

		new NativeReleaseLayoutChore().doit(extension.choreContext());

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

		new NativeReleaseLayoutChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.contains("${ARTIFACT}-${os}-${arch}.tar.gz")
				.contains("${ARTIFACT}-windows-x64.zip")
				.doesNotContain("\"artifacts/${ARTIFACT}-linux\"");
	}

	@Test
	public void testLeavesAWorkflowThatBuildsNoNativeImage() {
		String noGraal = NATIVE_CLI.replace("graalvm", "temurin");
		Path workflow = writeMain(noGraal);

		new NativeReleaseLayoutChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(noGraal);
	}

	@Test
	public void testLeavesAReleaseThatCollectsNoBinaries() {
		String noDownload = NATIVE_CLI
				.replace("      uses: actions/download-artifact@v8\n", "");
		Path workflow = writeMain(noDownload);

		new NativeReleaseLayoutChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(noDownload);
	}

	@Test
	public void testIsIdempotent() {
		Path workflow = writeMain(NATIVE_CLI);
		new NativeReleaseLayoutChore().doit(extension.choreContext());
		String once = FilesSilent.readString(workflow);

		new NativeReleaseLayoutChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(once);
	}

	@Test
	public void testWithNothing() {
		new NativeReleaseLayoutChore().doit(extension.choreContext());

		assertThat(extension.relativePaths()).isEmpty();
	}

}
