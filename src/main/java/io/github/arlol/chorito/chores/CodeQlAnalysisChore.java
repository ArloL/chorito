package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.GitHubActionsWorkflowFile;
import io.github.arlol.chorito.tools.RandomCronBuilder;

public class CodeQlAnalysisChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		if (context.hasGitHubRemote()
				&& FilesSilent.exists(context.resolve("pom.xml"))) {
			RandomCronBuilder randomCronBuilder = new RandomCronBuilder(
					context.randomGenerator()
			);
			var template = new GitHubActionsWorkflowFile(
					ClassPathFiles.readString(
							"github-settings/workflows/codeql-analysis.yaml"
					)
			);
			template.setOnScheduleCron(randomCronBuilder.randomDayOfMonth());

			Path codeqlWorkflow = context
					.resolve(".github/workflows/codeql-analysis.yaml");
			if (FilesSilent.exists(codeqlWorkflow)) {
				var workflowFile = new GitHubActionsWorkflowFile(
						FilesSilent.readString(codeqlWorkflow)
				);
				template.setOn(workflowFile.getOn());
				template.setEnv(workflowFile.getEnv());
			}
			FilesSilent.writeString(codeqlWorkflow, template.asString());
		}
		return context;
	}

}
