package io.github.arlol.chorito.chores;

import java.nio.file.Path;
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
		useSpecificActionVersions();
	}

	private void ensureYamlFileExtension() {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
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

	private void useSpecificActionVersions() {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			String updated = FilesSilent.readString(path);
			updated = updated.replace(
					"uses: actions/checkout@v3\n",
					"uses: actions/checkout@v3.1.0\n"
			);
			updated = updated.replace(
					"peter-evans/create-pull-request@v4\n",
					"peter-evans/create-pull-request@v4.1.3\n"
			);
			updated = updated.replace(
					"uses: actions/setup-java@v3\n",
					"uses: actions/setup-java@v3.5.1\n"
			);
			updated = updated.replace(
					"uses: github/codeql-action/init@v2\n",
					"uses: github/codeql-action/init@v2.1.27\n"
			);
			updated = updated.replace(
					"uses: github/codeql-action/autobuild@v2\n",
					"uses: github/codeql-action/autobuild@v2.1.27\n"
			);
			updated = updated.replace(
					"uses: github/codeql-action/analyze@v2\n",
					"uses: github/codeql-action/analyze@v2.1.27\n"
			);
			updated = updated.replace(
					"uses: mathieudutour/github-tag-action@v6.0\n",
					"uses: mathieudutour/github-tag-action@v6.0\n"
			);
			updated = updated.replace(
					"uses: actions/upload-artifact@v3\n",
					"uses: actions/upload-artifact@v3.1.0\n"
			);
			updated = updated.replace(
					"uses: actions/download-artifact@v3\n",
					"uses: actions/download-artifact@v3.0.0\n"
			);
			updated = updated.replace(
					"uses: eregon/publish-release@v1\n",
					"uses: eregon/publish-release@v1.0.4\n"
			);
			FilesSilent.writeString(path, updated);
		});

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
						if (s.contains(
								" --settings .\\.github\\github-actions-windows-maven-settings.xml"
						)) {
							return s.replace(
									" --settings .\\.github\\github-actions-windows-maven-settings.xml",
									""
							);
						}
						return s;
					})
					.toList();
			FilesSilent.write(main, updated);
		}
	}

}
