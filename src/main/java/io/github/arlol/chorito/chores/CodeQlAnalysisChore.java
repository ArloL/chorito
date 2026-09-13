package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.GitHubActionsWorkflowFile;
import io.github.arlol.chorito.tools.JavaVersions;
import io.github.arlol.chorito.tools.MyPaths;
import io.github.arlol.chorito.tools.RandomCronBuilder;
import io.github.arlol.chorito.tools.Template;
import io.github.arlol.chorito.tools.WorkflowJobs;

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
				.anyMatch(path -> path.endsWith("pom.xml"))) {
			languages.add("java-kotlin");
		}
		if (context.textFiles()
				.stream()
				.anyMatch(path -> path.endsWith("package.json"))) {
			languages.add("javascript-typescript");
		}
		if (context.textFiles()
				.stream()
				.anyMatch(
						path -> path.endsWith("Pipfile")
								|| path.endsWith("pyproject.toml")
				)) {
			languages.add("python");
		}
		if (context.textFiles()
				.stream()
				.anyMatch(path -> path.endsWith("go.mod"))) {
			languages.add("go");
		}
		languages.add("actions");
		languages.sort(Comparator.naturalOrder());

		RandomCronBuilder randomCronBuilder = new RandomCronBuilder(
				context.randomGenerator()
		);
		var template = Template.codeQlAnalysisWorkflow();
		template.setOnScheduleCron(randomCronBuilder.randomDayOfMonth());

		Path codeqlWorkflow = context
				.resolve(".github/workflows/codeql-analysis.yaml");
		String before = "";
		Optional<String> pinnedJavaVersion = Optional.empty();
		if (FilesSilent.exists(codeqlWorkflow)) {
			var workflowFile = new GitHubActionsWorkflowFile(
					FilesSilent.readString(codeqlWorkflow)
			);
			before = workflowFile.asStringWithoutVersions();
			template.setOn(workflowFile.getOn());
			template.setEnv(workflowFile.getEnv());
			pinnedJavaVersion = workflowFile.getPinnedJavaVersion();
		} else {
		}

		if (!languages.contains("java-kotlin")) {
			template.removeActionFromJob(
					WorkflowJobs.ANALYZE,
					"actions/setup-java"
			);
			template.removeEnv();
		}

		// Analysis builds no native image, so .tool-versions would install a
		// GraalVM it never uses. Renovate owns the pin once it is written.
		// Template hands over a workflow already pointing at .tool-versions,
		// so only the repositories that need a pin get one.
		if (JavaVersions.buildsOnGraalVm(context)) {
			template.pinTemurinJavaVersion(
					pinnedJavaVersion.orElse(JavaVersions.TEMURIN)
			);
		}

		template.setJobMatrixKey(WorkflowJobs.ANALYZE, "language", languages);

		if (!template.asStringWithoutVersions().equals(before)) {
			FilesSilent.writeString(codeqlWorkflow, template.asString());
		}

		return context;
	}

}
