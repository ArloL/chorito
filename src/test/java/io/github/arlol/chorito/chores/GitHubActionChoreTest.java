package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FakeRandomGenerator;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

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
			jobs:
			  linux:
			    runs-on: ubuntu-latest
			    steps:
			    - uses: actions/setup-java@v3.5.1
			      with:
			        distribution: temurin
			""";
	private static final String INPUT_GRAALSETUP_OUTPUT = """
			jobs:
			  linux:
			    runs-on: ubuntu-latest
			    needs: version
			    env:
			      REVISION: ${{ needs.version.outputs.new_version }}
			    steps:
			    - uses: actions/checkout@v3.1.0
			    - uses: actions/setup-java@v3.5.1
			      with:
			        java-version: ${{ env.JAVA_VERSION }}
			        distribution: adopt
			        cache: 'maven'
			    - name: Setup Graalvm
			      uses: DeLaGuardo/setup-graalvm@5.0
			      with:
			        graalvm: ${{ env.GRAALVM_VERSION }}
			        java: java${{ env.JAVA_VERSION }}
			    - name: Install native-image module
			      run: gu install native-image
			  windows:
			    runs-on: windows-latest
			    needs: version
			    env:
			      REVISION: ${{ needs.version.outputs.new_version }}
			    steps:
			    - uses: actions/checkout@v3.1.0
			    - uses: actions/setup-java@v3.5.1
			      with:
			        java-version: ${{ env.JAVA_VERSION }}
			        distribution: adopt
			        cache: 'maven'
			    - name: Setup Graalvm
			      uses: DeLaGuardo/setup-graalvm@5.0
			      with:
			        graalvm: ${{ env.GRAALVM_VERSION }}
			        java: java${{ env.JAVA_VERSION }}
			    - name: Install native-image module
			      run: '& "$env:JAVA_HOME\\bin\\gu" install native-image'
			    - name: Remove WindowsImageHeapProviderFeature
			      run: '& 7z d "$env:JAVA_HOME\\lib\\svm\\builder\\svm.jar" com/oracle/svm/core/windows/WindowsImageHeapProviderFeature.class'
			    - name: Install upx
			      run: choco install upx --version=3.96 --no-progress
			    - name: Set up Visual Studio shell
			      uses: egor-tensin/vs-shell@v2
			""";
	private static final String EXPECTED_GRAALSETUP_OUTPUT = """
			jobs:
			  linux:
			    runs-on: ubuntu-latest
			    needs: version
			    env:
			      REVISION: ${{ needs.version.outputs.new_version }}
			    steps:
			    - uses: actions/checkout@v3.1.0
			    - uses: graalvm/setup-graalvm@v1.0.7
			      with:
			        version: ${{ env.GRAALVM_VERSION }}
			        java-version: ${{ env.JAVA_VERSION }}
			        components: 'native-image'
			        github-token: ${{ secrets.GITHUB_TOKEN }}
			        cache: 'maven'
			  windows:
			    runs-on: windows-latest
			    needs: version
			    env:
			      REVISION: ${{ needs.version.outputs.new_version }}
			    steps:
			    - uses: actions/checkout@v3.1.0
			    - uses: graalvm/setup-graalvm@v1.0.7
			      with:
			        version: ${{ env.GRAALVM_VERSION }}
			        java-version: ${{ env.JAVA_VERSION }}
			        components: 'native-image'
			        github-token: ${{ secrets.GITHUB_TOKEN }}
			        cache: 'maven'
			    - name: Remove WindowsImageHeapProviderFeature
			      run: '& 7z d "$env:JAVA_HOME\\lib\\svm\\builder\\svm.jar" com/oracle/svm/core/windows/WindowsImageHeapProviderFeature.class'
			    - name: Install upx
			      run: choco install upx --version=3.96 --no-progress
			""";

	private static final String INPUT_STEPS_OUTPUT = """
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
	private static final String EXPECTED_STEPS_OUTPUT = """
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
	public void testEmptyWorkflowFile() throws Exception {
		ChoreContext context = extension.choreContext();
		Path workflow = context.resolve(".github/workflows/main.yaml");
		FilesSilent.touch(workflow);
		assertTrue(FilesSilent.exists(workflow));
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow)).isEqualTo("");
	}

	@Test
	public void testBasicWorkflowFile() throws Exception {
		ChoreContext context = extension.choreContext();
		Path workflow = context.resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				workflow,
				"jobs:\n" + "  linux:\n" + "    runs-on: ubuntu-latest\n"
						+ "    steps:\n" + "    - run: whoami\n"
		);
		assertTrue(FilesSilent.exists(workflow));
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow)).isEqualTo(
				"jobs:\n" + "  linux:\n" + "    runs-on: ubuntu-latest\n"
						+ "    steps:\n" + "    - run: whoami\n"
		);
	}

	@Test
	public void testVsShellWorkflowFile() throws Exception {
		ChoreContext context = extension.choreContext();
		Path workflow = context.resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				workflow,
				"jobs:\n" + "  windows:\n" + "    runs-on: windows-latest\n"
						+ "    steps:\n"
						+ "    - name: Set up Visual Studio shell\n"
						+ "      uses: egor-tensin/vs-shell@v2\n"
		);
		assertTrue(FilesSilent.exists(workflow));
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow)).isEqualTo(
				"jobs:\n" + "  windows:\n" + "    runs-on: windows-latest\n"
						+ "    steps:\n"
		);
	}

	@Test
	public void testGraalSetupMigration() throws Exception {
		ChoreContext context = extension.choreContext();
		Path workflow = context.resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, INPUT_GRAALSETUP_OUTPUT);
		assertTrue(FilesSilent.exists(workflow));
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow))
				.isEqualTo(EXPECTED_GRAALSETUP_OUTPUT);
	}

	@Test
	public void testAdoptTemurinMigration() throws Exception {
		ChoreContext context = extension.choreContext();
		Path workflow = context.resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, INPUT_ADOPT);
		assertTrue(FilesSilent.exists(workflow));
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow)).isEqualTo(EXPECTED_ADOPT);
	}

	@Test
	public void testStepsOutput() throws Exception {
		ChoreContext context = extension.choreContext();
		Path workflow = context.resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, INPUT_STEPS_OUTPUT);
		assertTrue(FilesSilent.exists(workflow));
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow))
				.isEqualTo(EXPECTED_STEPS_OUTPUT);
	}

	@Test
	public void testChoresSetSchedule53445() throws Exception {
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.build();
		Path workflow = context.resolve(".github/workflows/chores.yaml");
		FilesSilent.writeString(workflow, "- cron: '5 3 4 4 5'");
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow)).startsWith("""
				name: Chores

				on:
				  workflow_dispatch:
				  repository_dispatch:
				    types:
				    - chores
				  schedule:
				  - cron: '5 3 4 4 5'
				""");
	}

	@Test
	public void testChoresSetSchedule26155() throws Exception {
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.build();
		Path workflow = context.resolve(".github/workflows/chores.yaml");
		FilesSilent.writeString(workflow, "- cron: '26 15 * * 5'");
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow)).doesNotStartWith("""
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
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.randomGenerator(new FakeRandomGenerator())
				.build();
		Path workflow = context
				.resolve(".github/workflows/codeql-analysis.yaml");
		String input = "- cron: '4 20 * * 3'";
		FilesSilent.writeString(workflow, input);
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow))
				.isEqualTo("- cron: '1 3 1 * *'\n");
	}

	@Test
	public void testMainSchedulDuplicated() throws Exception {
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.randomGenerator(new FakeRandomGenerator())
				.build();
		Path workflow = context.resolve(".github/workflows/main.yaml");
		String input = "- cron: '17 4 5 * *'";
		FilesSilent.writeString(workflow, input);
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow))
				.isEqualTo("- cron: '1 3 1 * *'\n");
	}

	@Test
	public void testMainScheduleAlreadyReplaced() throws Exception {
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.randomGenerator(new FakeRandomGenerator())
				.build();
		Path workflow = context.resolve(".github/workflows/main.yaml");
		String input = "- cron: '5 5 5 * *'";
		FilesSilent.writeString(workflow, input);
		new GitHubActionChore(context.refresh()).doit();
		assertThat(FilesSilent.readString(workflow))
				.isEqualTo("- cron: '5 5 5 * *'\n");
	}

}
