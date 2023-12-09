package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class DependabotChore implements Chore {

	private static String DEFAULT_DEPENDABOT = """
			version: 2
			updates:
			""";
	private static String DEFAULT_MAVEN_DEPENDABOT = """
			  - package-ecosystem: "maven"
			    directory: "/"
			    schedule:
			      interval: "daily"
			""";
	private static String DEFAULT_GITHUB_ACTIONS_DEPENDABOT = """
			  - package-ecosystem: "github-actions"
			    directory: "/"
			    schedule:
			      interval: "daily"
			""";
	private static String DEFAULT_BUNDLER_DEPENDABOT = """
			  - package-ecosystem: "bundler"
			    directory: "/"
			    schedule:
			      interval: "daily"
			""";
	private static String DEFAULT_DOCKER_DEPENDABOT = """
			  - package-ecosystem: "docker"
			    directory: "/"
			    schedule:
			      interval: "daily"
			""";
	private static String DEFAULT_PIP_DEPENDABOT = """
			  - package-ecosystem: "pip"
			    directory: "/"
			    schedule:
			      interval: "daily"
			""";
	private static String DEFAULT_NPM_DEPENDABOT = """
			  - package-ecosystem: "npm"
			    directory: "/"
			    schedule:
			      interval: "daily"
			""";
	private static String DEFAULT_GRADLE_DEPENDABOT = """
			  - package-ecosystem: "gradle"
			    directory: "/"
			    schedule:
			      interval: "daily"
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
		if (context.hasGitHubRemote()) {
			String content = DEFAULT_DEPENDABOT;
			if (FilesSilent.exists(context.resolve("pom.xml"))) {
				content += DEFAULT_MAVEN_DEPENDABOT;
			}
			if (FilesSilent.exists(context.resolve("Gemfile.lock"))) {
				content += DEFAULT_BUNDLER_DEPENDABOT;
			}
			content += DEFAULT_GITHUB_ACTIONS_DEPENDABOT;
			if (FilesSilent.exists(context.resolve("Dockerfile"))) {
				content += DEFAULT_DOCKER_DEPENDABOT;
			}
			if (FilesSilent.exists(context.resolve("Pipfile"))) {
				content += DEFAULT_PIP_DEPENDABOT;
			}
			if (FilesSilent.exists(context.resolve("package.json"))) {
				content += DEFAULT_NPM_DEPENDABOT;
			}
			if (FilesSilent.exists(context.resolve("build.gradle"))) {
				content += DEFAULT_GRADLE_DEPENDABOT;
			}
			FilesSilent.writeString(
					context.resolve(".github/dependabot.yml"),
					content
			);
		}
		return context;
	}

}
