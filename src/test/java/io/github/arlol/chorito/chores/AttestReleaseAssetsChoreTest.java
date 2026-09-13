package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class AttestReleaseAssetsChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private static final String PUBLISHES_ASSETS = """
			jobs:
			  release:
			    runs-on: ubuntu-latest
			    permissions:
			      contents: write
			    steps:
			    - name: Prepare artifacts
			      run: mkdir artifacts
			    - name: Create Release
			      run: |
			        gh release create "v1" ./target/artifacts/*
			    - name: Make sure build did not change anything
			      run: git diff --exit-code
			""";

	private static final String PUBLISHES_NOTHING = """
			jobs:
			  release:
			    runs-on: ubuntu-latest
			    permissions:
			      contents: write
			    steps:
			    - name: Create Release
			      run: |
			        gh release create "v1"
			""";

	private Path writeMain(String content) {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, content);
		return workflow;
	}

	@Test
	public void testAttestsAReleaseThatPublishesAssets() {
		Path workflow = writeMain(PUBLISHES_ASSETS);

		new AttestReleaseAssetsChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.isEqualTo(
						"""
								jobs:
								  release:
								    runs-on: ubuntu-latest
								    permissions:
								      attestations: write
								      contents: write
								      id-token: write
								    steps:
								    - name: Prepare artifacts
								      run: mkdir artifacts
								    - name: Attest the release assets
								      # An installer that verifies provenance cannot be told to trust an asset
								      # it cannot verify, so the attestation has to exist before the release
								      # does.
								      uses: actions/attest-build-provenance@4d101475d8b20a2381f78447822ac1eab6504dd8 # v4.2.2
								      with:
								        subject-path: target/artifacts/*
								    - name: Create Release
								      run: |
								        gh release create "v1" ./target/artifacts/*
								    - name: Make sure build did not change anything
								      run: git diff --exit-code
								"""
				);
	}

	@Test
	public void testLeavesAReleaseThatPublishesNothing() {
		Path workflow = writeMain(PUBLISHES_NOTHING);

		new AttestReleaseAssetsChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(PUBLISHES_NOTHING);
	}

	@Test
	public void testIsIdempotent() {
		Path workflow = writeMain(PUBLISHES_ASSETS);
		new AttestReleaseAssetsChore().doit(extension.choreContext());
		String once = FilesSilent.readString(workflow);

		new AttestReleaseAssetsChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(once);
	}

	@Test
	public void testWithNothing() {
		new AttestReleaseAssetsChore().doit(extension.choreContext());

		assertThat(extension.relativePaths()).isEmpty();
	}

}
