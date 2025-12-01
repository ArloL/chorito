package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class DependabotChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/ArloL/chorito.git"))
				.remotes(List.of("https://github.com/example/example"))
				.build();
		new DependabotChore().doit(context);
	}

	@Test
	public void testWithNothing() {
		doit();
	}

	@Test
	public void testPackageJson() throws Exception {
		FilesSilent.touch(
				extension.root()
						.resolve("test-projects/vite-project/package.json")
		);

		doit();

		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		assertThat(dependabot).content().isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "github-actions"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				- package-ecosystem: "npm"
				  directory: "/test-projects/vite-project/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				""");
	}

	@Test
	public void testTwoPackageJsons() throws Exception {
		FilesSilent.touch(extension.root().resolve("package.json"));
		FilesSilent.touch(
				extension.root()
						.resolve("test-projects/vite-project/package.json")
		);

		doit();

		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		assertThat(dependabot).content().isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "github-actions"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				- package-ecosystem: "npm"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				- package-ecosystem: "npm"
				  directory: "/test-projects/vite-project/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				""");
	}

	@Test
	public void testTerraform() throws Exception {
		FilesSilent.touch(extension.root().resolve(".terraform.lock.hcl"));

		doit();

		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		assertThat(dependabot).content().isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "github-actions"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				- package-ecosystem: "terraform"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				""");
	}

	@Test
	public void testDockerfile() throws Exception {
		FilesSilent.touch(extension.root().resolve("random.Dockerfile"));

		doit();

		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		assertThat(dependabot).content().isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "github-actions"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				- package-ecosystem: "docker"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				""");
	}

	@Test
	public void testDockerfileLowercase() throws Exception {
		FilesSilent.touch(extension.root().resolve("random.dockerfile"));

		doit();

		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		assertThat(dependabot).content().isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "github-actions"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				- package-ecosystem: "docker"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				""");
	}

	@Test
	public void testKeepExistingSettings() throws Exception {
		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		FilesSilent.writeString(dependabot, """
				version: 2
				updates:
				  - package-ecosystem: "github-actions"
				    open-pull-requests-limit: 1
				    directory: "/"
				    schedule:
				      interval: "monthly"
				    cooldown:
				      default-days: 7
				""");

		doit();

		assertThat(dependabot).content().isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "github-actions"
				  open-pull-requests-limit: 1
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				""");
	}

	@Test
	public void testGitHubActionsComposite() throws Exception {
		String compositeAction = """
				name: hello

				inputs:
				  name:
				    required: true
				    type: string

				runs:
				  using: composite
				  steps:
				    - shell: bash
				      run: |
				        echo "Hello ${{ inputs.name }}"
				""";
		FilesSilent.writeString(
				extension.root().resolve(".github/actions/hello/action.yml"),
				compositeAction
		);
		FilesSilent.writeString(
				extension.root().resolve(".github/actions/bye/action.yaml"),
				compositeAction
		);

		doit();

		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		assertThat(dependabot).content().isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "github-actions"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				- package-ecosystem: "github-actions"
				  directory: "/.github/actions/hello/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				- package-ecosystem: "github-actions"
				  directory: "/.github/actions/bye/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				""");
	}

	@Test
	public void testGitHubActionsNode() throws Exception {
		String nodeAction = """
				name: 'Hello World'
				description: 'Greet someone and record the time'
				inputs:
				  who-to-greet:  # id of input
				    description: 'Who to greet'
				    required: true
				    default: 'World'
				outputs:
				  time: # id of output
				    description: 'The time we greeted you'
				runs:
				  using: 'node20'
				  main: 'index.js'
				""";
		FilesSilent.writeString(
				extension.root().resolve(".github/actions/hello/actions.yml"),
				nodeAction
		);

		doit();

		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		assertThat(dependabot).content().isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "github-actions"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				""");
	}

	@Test
	public void testMaintainSettingsWithTrailingSlash() throws Exception {
		// given
		FilesSilent.touch(extension.root().resolve("subdir/pom.xml"));
		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		FilesSilent.writeString(dependabot, """
				version: 2
				updates:
				  - package-ecosystem: "maven"
				    open-pull-requests-limit: 1
				    directory: "/subdir"
				    schedule:
				      interval: "monthly"
				""");

		// when
		doit();

		assertThat(dependabot).content().isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "maven"
				  open-pull-requests-limit: 1
				  directory: "/subdir"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				- package-ecosystem: "github-actions"
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				""");
	}

	@Test
	public void testUpdateIntervalToMonthly() throws Exception {
		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		FilesSilent.writeString(dependabot, """
				version: 2
				updates:
				  - package-ecosystem: "github-actions"
				    open-pull-requests-limit: 1
				    directory: "/"
				    schedule:
				      interval: "daily"
				""");

		doit();

		assertThat(dependabot).content().isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "github-actions"
				  open-pull-requests-limit: 1
				  directory: "/"
				  schedule:
				    interval: "monthly"
				  cooldown:
				    default-days: 7
				""");
	}

}
