package io.github.arlol.chorito.chores;

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

	private String removeVersions(String input) {
		return input.replaceAll("@v[0-9.]+\n", "@\n");
	}

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new GitHubActionChore().doit(extension.choreContext());
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

		FilesSilent.writeString(
				workflow,
				"jobs:\n" + "  linux:\n" + "    runs-on: ubuntu-latest\n"
						+ "    steps:\n" + "    - run: whoami\n"
		);

		new GitHubActionChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.isEqualTo(
						"jobs:\n" + "  linux:\n"
								+ "    runs-on: ubuntu-latest\n"
								+ "    steps:\n" + "    - run: whoami\n"
				);
	}

	@Test
	public void testVsShellWorkflowFile() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				workflow,
				"jobs:\n" + "  windows:\n" + "    runs-on: windows-latest\n"
						+ "    steps:\n"
						+ "    - name: Set up Visual Studio shell\n"
						+ "      uses: egor-tensin/vs-shell@v2\n"
		);

		new GitHubActionChore().doit(extension.choreContext());

		assertThat(workflow).content()
				.isEqualTo(
						"jobs:\n" + "  windows:\n"
								+ "    runs-on: windows-latest\n"
								+ "    steps:\n"
				);
	}

	@Test
	public void testGraalSetupMigration() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, INPUT_GRAALSETUP_OUTPUT);

		new GitHubActionChore().doit(extension.choreContext());

		assertThat(workflow).content().isEqualTo(EXPECTED_GRAALSETUP_OUTPUT);
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
				  - cron: '5 3 4 4 5'
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
				  - cron: '5 3 4 4 5'
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
		String input = "- cron: '4 20 * * 3'";
		FilesSilent.writeString(workflow, input);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().isEqualTo("- cron: '1 3 1 * *'\n");
	}

	@Test
	public void testMainSchedulDuplicated() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		String input = "- cron: '17 4 5 * *'";
		FilesSilent.writeString(workflow, input);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().isEqualTo("- cron: '1 3 1 * *'\n");
	}

	@Test
	public void testMainScheduleAlreadyReplaced() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		String input = "- cron: '5 5 5 * *'";
		FilesSilent.writeString(workflow, input);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		assertThat(workflow).content().isEqualTo("- cron: '5 5 5 * *'\n");
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
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		String expected = ClassPathFiles
				.readString("github-actions/no-empty-lines-output.yaml");
		assertThat(pom).content().isEqualTo(expected);
	}

	@Test
	public void testTestExectuableCreation() {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				workflow,
				ClassPathFiles
						.readString("github-actions/upx-removal-output.yaml")
		);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		Path testExecutable = extension.root()
				.resolve("src/test/native/test-executable.sh");
		String expected = ClassPathFiles
				.readString("github-actions/test-executable-expected.sh");
		assertThat(testExecutable).content().isEqualTo(expected);
	}

	@Test
	public void testTestExectuableUpdate() {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(
				workflow,
				ClassPathFiles
						.readString("github-actions/upx-removal-output.yaml")
		);
		Path testExecutable = extension.root()
				.resolve("src/test/native/test-executable.sh");
		FilesSilent.writeString(testExecutable, """
				lol
				# add custom tests here:
				lololol
				""");

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new GitHubActionChore().doit(context);

		String expected = ClassPathFiles.readString(
				"github-actions/test-executable-expected-update.sh"
		);
		assertThat(testExecutable).content().isEqualTo(expected);
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
				jobs:
				  analyze:
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
				.randomGenerator(new FakeRandomGenerator())
				.build();
		new GitHubActionChore().doit(context);
		assertThat(workflow).content().isEqualTo(outdatedWorkflow);
	}

}
