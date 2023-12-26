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
				.hasGitHubRemote(true)
				.build();
		new DependabotChore().doit(context);
	}

	@Test
	public void testWithNothing() {
		doit();
	}

	@Test
	public void test() throws Exception {
		FilesSilent.touch(
				extension.root()
						.resolve("test-projects/vite-project/package.json")
		);

		doit();

		Path dependabot = extension.root().resolve(".github/dependabot.yml");
		assertThat(FilesSilent.readString(dependabot)).isEqualTo("""
				version: 2
				updates:
				  - package-ecosystem: "github-actions"
				    directory: "/"
				    schedule:
				      interval: "daily"
				  - package-ecosystem: "npm"
				    directory: "/test-projects/vite-project/"
				    schedule:
				      interval: "daily"
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
		assertThat(FilesSilent.readString(dependabot)).isEqualTo("""
				version: 2
				updates:
				  - package-ecosystem: "github-actions"
				    directory: "/"
				    schedule:
				      interval: "daily"
				  - package-ecosystem: "npm"
				    directory: "/"
				    schedule:
				      interval: "daily"
				  - package-ecosystem: "npm"
				    directory: "/test-projects/vite-project/"
				    schedule:
				      interval: "daily"
				""");
	}

}
