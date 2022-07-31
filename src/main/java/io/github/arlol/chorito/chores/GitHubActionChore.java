package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.Renamer;

public class GitHubActionChore {

	private final ChoreContext context;

	public GitHubActionChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		ensureYamlFileExtension();
		updateChoresWorkflow();
		updateGraalVmVersion();
		removeCustomGithubPackagesMavenSettings();
	}

	private void ensureYamlFileExtension() {
		Path workflowsLocation = Paths.get(".github/workflows");
		context.files().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yml");
			}
			return false;
		})
				.map(context::resolve)
				.forEach(
						path -> Renamer.replaceInFilename(path, ".yml", ".yaml")
				);
	}

	private void updateChoresWorkflow() {
		if (context.hasGitHubRemote()) {
			FilesSilent.writeString(
					context.resolve(".github/workflows/chores.yaml"),
					ClassPathFiles.readString("/workflows/chores.yaml")
			);
		}
	}

	private void updateGraalVmVersion() {
		Path main = context.resolve(".github/workflows/main.yaml");
		if (FilesSilent.exists(main)) {
			List<String> updated = FilesSilent.readAllLines(main)
					.stream()
					.map(s -> {
						if (s.startsWith("  GRAALVM_VERSION: ")) {
							return "  GRAALVM_VERSION: 22.2.0";
						}
						return s;
					})
					.toList();
			FilesSilent.write(main, updated);
		}
	}

	private void removeCustomGithubPackagesMavenSettings() {
		Path main = context.resolve(".github/workflows/main.yaml");
		if (FilesSilent.exists(main)) {
			List<String> updated = FilesSilent.readAllLines(main)
					.stream()
					.map(s -> {
						if (s.startsWith(
								"          --settings ./.github/github-packages-maven-settings.xml \\"
						)) {
							return "          \\";
						}
						return s;
					})
					.toList();
			FilesSilent.write(main, updated);
		}
	}

}
