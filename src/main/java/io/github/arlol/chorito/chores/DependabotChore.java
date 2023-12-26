package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.function.Predicate;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

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
			content += getEcosystemIfFileNameMatches(
					context,
					"(?i).*dockerfile.*",
					"docker"
			);
			content += getEcosystemIfFileExists(context, "Pipfile", "pip");
			content += getEcosystemIfFileExists(context, "package.json", "npm");
			content += getEcosystemIfFileExists(
					context,
					"build.gradle",
					"gradle"
			);
			content += getEcosystemIfFileExists(
					context,
					".terraform.lock.hcl",
					"terraform"
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
		return getEcosystemIfFilterMatches(context, path -> {
			return path.endsWith(fileName);
		}, ecosystem);
	}

	private String getEcosystemIfFileNameMatches(
			ChoreContext context,
			String fileNamePattern,
			String ecosystem
	) {
		return getEcosystemIfFilterMatches(context, path -> {
			return MyPaths.getFileName(path)
					.toString()
					.matches(fileNamePattern);
		}, ecosystem);
	}

	private String getEcosystemIfFilterMatches(
			ChoreContext context,
			Predicate<? super Path> predicate,
			String ecosystem
	) {
		StringBuilder result = new StringBuilder();
		context.textFiles()
				.stream()
				.filter(predicate)
				.map(context::resolve)
				.map(path -> getRootRelativePath(context.root(), path))
				.distinct()
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
