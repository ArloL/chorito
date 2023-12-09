package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.RandomCronBuilder;

public class CodeQlAnalysisChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		RandomCronBuilder randomCronBuilder = new RandomCronBuilder(
				context.randomGenerator()
		);
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
		return context;
	}

}
