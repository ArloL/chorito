package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.GitHubActionsWorkflowFile;
import io.github.arlol.chorito.tools.MyPaths;
import io.github.arlol.chorito.tools.RandomCronBuilder;

public class CodeQlAnalysisChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		// it only makes sense to add it to github repositories
		if (context.remotes()
				.stream()
				.noneMatch(s -> s.startsWith("https://github.com"))
				&& context.textFiles()
						.stream()
						.map(MyPaths::getParent)
						.noneMatch(path -> path.endsWith(".github"))) {
			return context;
		}

		List<String> languages = new ArrayList<>();

		if (context.textFiles()
				.stream()
				.anyMatch(p -> MyPaths.getFileName(p).endsWith("pom.xml"))) {
			languages.add("java-kotlin");
		}
		if (context.textFiles()
				.stream()
				.anyMatch(
						p -> MyPaths.getFileName(p).endsWith("package.json")
				)) {
			languages.add("javascript-typescript");
		}
		if (context.textFiles()
				.stream()
				.anyMatch(p -> MyPaths.getFileName(p).endsWith("Pipfile"))) {
			languages.add("python");
		}

		// there is no code in this repository that can be analyzed
		if (languages.isEmpty()) {
			return context;
		}

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
		String before = "";
		if (FilesSilent.exists(codeqlWorkflow)) {
			var workflowFile = new GitHubActionsWorkflowFile(
					FilesSilent.readString(codeqlWorkflow)
			);
			before = workflowFile.asStringWithoutVersions();
			template.setOn(workflowFile.getOn());
			template.setEnv(workflowFile.getEnv());
		} else {
			context.setDirty();
		}

		if (!languages.contains("java-kotlin")) {
			template.removeActionFromJob("analyze", "actions/setup-java");
			template.removeEnv();
		}

		template.setJobMatrixKey("analyze", "language", languages);

		if (!template.asStringWithoutVersions().equals(before)) {
			FilesSilent.writeString(codeqlWorkflow, template.asString());
		}

		return context;
	}

}
