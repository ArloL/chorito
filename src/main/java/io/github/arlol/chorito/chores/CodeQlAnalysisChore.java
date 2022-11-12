package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.RandomCronBuilder;

public class CodeQlAnalysisChore {

	private final ChoreContext context;
	private final RandomCronBuilder randomCronBuilder;

	public CodeQlAnalysisChore(ChoreContext context) {
		this.context = context;
		this.randomCronBuilder = new RandomCronBuilder(
				context.randomGenerator()
		);
	}

	public void doit() {
		if (context.hasGitHubRemote()) {
			String currentAnalysis = ClassPathFiles
					.readString("/workflows/codeql-analysis.yaml");
			Path codeQlAnalysis = context
					.resolve(".github/workflows/codeql-analysis.yaml");
			if (!FilesSilent.exists(codeQlAnalysis)) {
				if (FilesSilent.exists(context.resolve("pom.xml"))) {
					FilesSilent.writeString(
							codeQlAnalysis,
							currentAnalysis.replace(
									"cron: '54 17 16 * *'",
									"cron: '" + randomCronBuilder
											.randomDayOfMonth() + "'"
							)
					);
				}
			}
		}
	}

}
