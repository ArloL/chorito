package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitHubActionChoreTest {

	private static final String INPUT = """
			jobs:
			  version:
			    runs-on: ubuntu-latest
			    outputs:
			      new_version: ${{ steps.output.outputs.new_version }}
			    steps:
			    - name: Bump version and push tag
			      id: tag
			      if: ${{ github.ref == 'refs/heads/master' || github.ref == 'refs/heads/main' }}
			      uses: mathieudutour/github-tag-action@v6.0
			      with:
			        github_token: ${{ secrets.GITHUB_TOKEN }}
			        release_branches: master,main
			    - id: output
			      env:
			        NEW_VERSION: ${{ steps.tag.outputs.new_version}}
			      run: |
			        echo "::set-output name=new_version::${NEW_VERSION:-$GITHUB_SHA}"
			  macos:
				runs-on: macos-latest
				needs: version
				env:
				  REVISION: ${{ needs.version.outputs.new_version }}
				steps:
				- name: Build with Maven
				  run: |
				    set -o xtrace
				    ./mvnw \
				      --batch-mode \
				      -Dsha1="${GITHUB_SHA}" \
				      -Drevision="${REVISION}" \
				      verify
			""";
	private static final String EXPECTED = """
			jobs:
			  version:
			    runs-on: ubuntu-latest
			    outputs:
			      new_version: ${{ steps.output.outputs.new_version }}
			    steps:
			    - name: Bump version and push tag
			      id: tag
			      if: ${{ github.ref == 'refs/heads/master' || github.ref == 'refs/heads/main' }}
			      uses: mathieudutour/github-tag-action@v6.0
			      with:
			        github_token: ${{ secrets.GITHUB_TOKEN }}
			        release_branches: master,main
			    - id: output
			      env:
			        NEW_VERSION: ${{ steps.tag.outputs.new_version}}
			      run: |
			        echo "new_version=${NEW_VERSION:-$GITHUB_SHA}" >> $GITHUB_OUTPUT
			  macos:
				runs-on: macos-latest
				needs: version
				env:
				  REVISION: ${{ needs.version.outputs.new_version }}
				steps:
				- name: Build with Maven
				  run: |
				    set -o xtrace
				    ./mvnw \
				      --batch-mode \
				      -Dsha1="${GITHUB_SHA}" \
				      -Drevision="${REVISION}" \
				      verify
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new GitHubActionChore(extension.choreContext()).doit();
	}

	@Test
	public void test() throws Exception {
		ChoreContext context = context();
		Path mainWorkflow = context.resolve(".github/workflows/main.yaml");
		assertTrue(FilesSilent.exists(mainWorkflow));
		new GitHubActionChore(context).doit();
		assertThat(Files.readString(mainWorkflow)).isEqualTo(EXPECTED);
	}

	private ChoreContext context() {
		ChoreContext context = extension.choreContext();
		FilesSilent.writeString(
				context.resolve(".github/workflows/main.yaml"),
				INPUT

		);
		return context.refresh();
	}

}
