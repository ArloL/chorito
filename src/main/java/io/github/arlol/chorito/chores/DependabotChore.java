package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class DependabotChore {

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

	private final ChoreContext context;

	public DependabotChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		String content = DEFAULT_DEPENDABOT;
		if (FilesSilent.exists(context.resolve("pom.xml"))) {
			content += DEFAULT_MAVEN_DEPENDABOT;
		}
		content += DEFAULT_GITHUB_ACTIONS_DEPENDABOT;
		FilesSilent.writeString(
				context.resolve(".github/dependabot.yml"),
				content
		);
	}

}