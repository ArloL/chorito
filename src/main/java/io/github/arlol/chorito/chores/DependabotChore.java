package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class DependabotChore implements Chore {

	private static String DEFAULT_GITHUB_ACTIONS_DEPENDABOT = """
			  - package-ecosystem: "github-actions"
			    directory: "/"
			    schedule:
			      interval: "daily"
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
		if (context.hasGitHubRemote()) {

			String content = """
					version: 2
					updates:
					""";
			content += getEcosystemIfFileExists(context, "pom.xml", "maven");
			content += getEcosystemIfFileExists(
					context,
					"Gemfile.lock",
					"bundler"
			);
			content += DEFAULT_GITHUB_ACTIONS_DEPENDABOT;
			content += getEcosystemIfFileExists(
					context,
					"Dockerfile",
					"docker"
			);
			content += getEcosystemIfFileExists(context, "Pipfile", "pip");
			content += getEcosystemIfFileExists(context, "package.json", "npm");
			content += getEcosystemIfFileExists(
					context,
					"build.gradle",
					"gradle"
			);

			FilesSilent.writeString(
					context.resolve(".github/dependabot.yml"),
					content
			);
		}
		return context;
	}

	private String getEcosystemIfFileExists(
			ChoreContext context,
			String fileName,
			String ecosystem
	) {
		StringBuilder result = new StringBuilder();
		context.textFiles().stream().filter(path -> {
			return path.endsWith(fileName);
		})
				.map(context::resolve)
				.map(path -> getRootRelativePath(context.root(), path))
				.forEach(rootRelativePath -> {
					result.append("""
							  - package-ecosystem: "%s"
							    directory: "%s"
							    schedule:
							      interval: "daily"
							""".formatted(ecosystem, rootRelativePath));
				});
		return result.toString();
	}

	private String getRootRelativePath(Path root, Path path) {
		String rootRelativePath = root.relativize(path.getParent()).toString();
		if (!rootRelativePath.startsWith("/")) {
			rootRelativePath = "/" + rootRelativePath;
		}
		if (!rootRelativePath.endsWith("/")) {
			rootRelativePath += "/";
		}
		return rootRelativePath;
	}

}
