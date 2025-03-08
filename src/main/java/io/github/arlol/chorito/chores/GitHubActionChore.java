package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.ExecutableFlagger;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.GitHubActionsWorkflowFile;
import io.github.arlol.chorito.tools.MyPaths;
import io.github.arlol.chorito.tools.RandomCronBuilder;

public class GitHubActionChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		updateChoresWorkflow(context);
		updateGraalVmVersion(context);
		removeCustomGithubPackagesMavenSettings(context);
		useSpecificActionVersions(context);
		replaceSetOutput(context);
		migrateToGraalSetupAction(context);
		migrateJavaDistributionFromAdoptToTemurin(context);
		updateCodeQlSchedule(context);
		updateMainSchedule(context);
		removeSetupJava370(context);
		migrateActionsCreateRelease(context);
		migrateActionsUploadReleaseAsset(context);
		updateGraalSteps(context);
		updateDebugSteps(context);
		updateVersionSteps(context);
		updatePermissions(context);
		return context;
	}

	public void updatePermissions(ChoreContext context) {
		Path mainYaml = context.resolve(".github/workflows/main.yaml");
		if (!FilesSilent.exists(mainYaml)) {
			return;
		}
		String string = FilesSilent.readString(mainYaml);
		var main = new GitHubActionsWorkflowFile(string);
		var template = new GitHubActionsWorkflowFile(
				ClassPathFiles.readString("github-settings/workflows/main.yaml")
		);
		main.updatePermissionsFromTemplate(template);
		FilesSilent.writeString(mainYaml, main.asString());
	}

	public void updateGraalSteps(ChoreContext context) {
		Path mainYaml = context.resolve(".github/workflows/main.yaml");
		if (!FilesSilent.exists(mainYaml)) {
			return;
		}
		String string = FilesSilent.readString(mainYaml);
		if (!string.contains("setup-graalvm") || string.contains("gluonfx")) {
			return;
		}
		var main = new GitHubActionsWorkflowFile(string);
		if (!main.hasJob("version")) {
			return;
		}
		updateTestExecutable(context);

		var currentMain = new GitHubActionsWorkflowFile(
				ClassPathFiles.readString("github-settings/workflows/main.yaml")
		);
		String before = main.asStringWithoutVersions();
		main.setJob("macos", currentMain.getJob("macos"));
		main.setJob("linux", currentMain.getJob("linux"));
		main.setJob("windows", currentMain.getJob("windows"));
		String after = main.asStringWithoutVersions();
		if (!after.equals(before)) {
			FilesSilent.writeString(mainYaml, main.asString());
		}
	}

	private void updateTestExecutable(ChoreContext context) {
		List<String> templateLines = ClassPathFiles
				.readAllLines("native-test/test-executable.sh");
		templateLines = templateLines.subList(
				0,
				templateLines.indexOf("# add custom tests here:") + 1
		);
		Path testExectuable = context
				.resolve("src/test/native/test-executable.sh");
		if (FilesSilent.exists(testExectuable)) {
			List<String> testExecutableLines = FilesSilent
					.readAllLines(testExectuable);
			testExecutableLines = testExecutableLines.subList(
					testExecutableLines.indexOf("# add custom tests here:") + 1,
					testExecutableLines.size()
			);
			templateLines.addAll(testExecutableLines);
		}
		FilesSilent.write(testExectuable, templateLines, "\n");
		ExecutableFlagger.makeExecutableIfPossible(testExectuable);
	}

	private void updateDebugSteps(ChoreContext context) {
		var currentMain = new GitHubActionsWorkflowFile(
				ClassPathFiles
						.readString("github-settings/workflows/chores.yaml")
		);
		var debugJob = currentMain.getJob("debug");
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			var workflow = new GitHubActionsWorkflowFile(
					FilesSilent.readString(path)
			);
			if (workflow.hasJob("debug")) {
				String before = workflow.asStringWithoutVersions();
				workflow.setJob("debug", debugJob);
				String after = workflow.asStringWithoutVersions();
				if (!after.equals(before)) {
					FilesSilent.writeString(path, workflow.asString());
				}
			}
		});
	}

	private void updateVersionSteps(ChoreContext context) {
		var currentMain = new GitHubActionsWorkflowFile(
				ClassPathFiles.readString("github-settings/workflows/main.yaml")
		);
		var versionJob = currentMain.getJob("version");
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			var workflow = new GitHubActionsWorkflowFile(
					FilesSilent.readString(path)
			);
			if (workflow.hasJob("version")) {
				String before = workflow.asStringWithoutVersions();
				workflow.setJob("version", versionJob);
				String after = workflow.asStringWithoutVersions();
				if (!after.equals(before)) {
					FilesSilent.writeString(path, workflow.asString());
				}
			}
		});
	}

	private void migrateActionsCreateRelease(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			String updated = FilesSilent.readString(path);
			String target = """
					uses: actions/create-release@v1.1.4
					      env:
					        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
					      with:
					        tag_name: v${{ needs.version.outputs.new_version }}
					        release_name: Release ${{ needs.version.outputs.new_version }}""";
			String replacement = """
					uses: ncipollo/release-action@v1.13.0
					      with:
					        tag: v${{ needs.version.outputs.new_version }}
					        name: Release ${{ needs.version.outputs.new_version }}""";
			updated = updated.replace(target, replacement);
			FilesSilent.writeString(path, updated);
		});
	}

	private void migrateActionsUploadReleaseAsset(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			String updated = FilesSilent.readString(path);
			String target = """
					uses: actions/upload-release-asset@v1.0.2
					      env:
					        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}""";
			updated = updated.replace(
					target,
					"uses: shogo82148/actions-upload-release-asset@v1.7.2"
			);
			FilesSilent.writeString(path, updated);
		});
	}

	private void updateChoresWorkflow(ChoreContext context) {
		RandomCronBuilder randomCronBuilder = new RandomCronBuilder(
				context.randomGenerator()
		);
		String randomDayOfMonth = randomCronBuilder.randomDayOfMonth();
		if (context.remotes()
				.stream()
				.anyMatch(s -> s.startsWith("https://github.com"))
				|| context.textFiles()
						.stream()
						.map(MyPaths::getParent)
						.anyMatch(path -> path.endsWith(".github"))) {
			Path choresYaml = context.resolve(".github/workflows/chores.yaml");

			var templateWorkflow = new GitHubActionsWorkflowFile(
					ClassPathFiles
							.readString("github-settings/workflows/chores.yaml")
			);

			GitHubActionsWorkflowFile choresWorkflow;
			if (FilesSilent.exists(choresYaml)) {
				choresWorkflow = new GitHubActionsWorkflowFile(
						FilesSilent.readString(choresYaml)
				);
				var cron = choresWorkflow.getOnScheduleCron()
						.filter(c -> !c.equals("26 15 * * 5"))
						.filter(c -> !c.equals("1 6 16 * *"))
						.orElse(randomDayOfMonth);
				templateWorkflow.setOnScheduleCron(cron);
			} else {
				choresWorkflow = templateWorkflow.copy();
				choresWorkflow.setOnScheduleCron(randomDayOfMonth);
			}

			if (!templateWorkflow.asStringWithoutVersions()
					.equals(choresWorkflow.asStringWithoutVersions())) {
				FilesSilent
						.writeString(choresYaml, templateWorkflow.asString());
			}
		}
	}

	private void updateCodeQlSchedule(ChoreContext context) {
		RandomCronBuilder randomCronBuilder = new RandomCronBuilder(
				context.randomGenerator()
		);
		String randomDayOfMonth = randomCronBuilder.randomDayOfMonth();
		Path yaml = context.resolve(".github/workflows/codeql-analysis.yaml");
		if (FilesSilent.exists(yaml)) {
			String content = FilesSilent.readString(yaml);
			String currentCron = readCurrentCron(content)
					.orElse(randomDayOfMonth);
			if (!currentCron.endsWith("*")) {
				FilesSilent.writeString(
						yaml,
						content.replace(currentCron, randomDayOfMonth)
				);
			}
		}
	}

	private void updateMainSchedule(ChoreContext context) {
		RandomCronBuilder randomCronBuilder = new RandomCronBuilder(
				context.randomGenerator()
		);
		String randomDayOfMonth = randomCronBuilder.randomDayOfMonth();
		Path yaml = context.resolve(".github/workflows/main.yaml");
		if (FilesSilent.exists(yaml)) {
			String content = FilesSilent.readString(yaml);
			readCurrentCron(content).ifPresent(currentCron -> {
				if (currentCron.equals("17 4 5 * *")) {
					FilesSilent.writeString(
							yaml,
							content.replace(currentCron, randomDayOfMonth)
					);
				}
			});
		}
	}

	private Optional<String> readCurrentCron(String yaml) {
		String startString = "cron: '";
		int indexOf = yaml.indexOf(startString);
		if (indexOf == -1) {
			return Optional.empty();
		}
		yaml = yaml.substring(indexOf + startString.length());
		indexOf = yaml.indexOf("'");
		if (indexOf == -1) {
			return Optional.empty();
		}
		return Optional.of(yaml.substring(0, indexOf));
	}

	private void migrateJavaDistributionFromAdoptToTemurin(
			ChoreContext context
	) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			String updated = FilesSilent.readString(path);
			updated = updated
					.replace("distribution: adopt", "distribution: temurin");
			updated = updated
					.replace("distribution: 'adopt'", "distribution: temurin");
			updated = updated.replace(
					"distribution: \"adopt\"",
					"distribution: temurin"
			);
			FilesSilent.writeString(path, updated);
		});
	}

	private void migrateToGraalSetupAction(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			String updated = FilesSilent.readString(path);
			updated = updated.replace(
					"\n" + "    - name: Set up Visual Studio shell\n"
							+ "      uses: egor-tensin/vs-shell@v2",
					""
			);
			updated = updated.replace(
					"    - uses: actions/setup-java@v3.5.1\n" + "      with:\n"
							+ "        java-version: ${{ env.JAVA_VERSION }}\n"
							+ "        distribution: adopt\n"
							+ "        cache: 'maven'\n"
							+ "    - name: Setup Graalvm\n"
							+ "      uses: DeLaGuardo/setup-graalvm@5.0\n"
							+ "      with:\n"
							+ "        graalvm: ${{ env.GRAALVM_VERSION }}\n"
							+ "        java: java${{ env.JAVA_VERSION }}\n"
							+ "    - name: Install native-image module\n"
							+ "      run: gu install native-image",
					"    - uses: graalvm/setup-graalvm@v1.0.7\n"
							+ "      with:\n"
							+ "        version: ${{ env.GRAALVM_VERSION }}\n"
							+ "        java-version: ${{ env.JAVA_VERSION }}\n"
							+ "        components: 'native-image'\n"
							+ "        github-token: ${{ secrets.GITHUB_TOKEN }}\n"
							+ "        cache: 'maven'"
			);
			updated = updated.replace(
					"    - uses: actions/setup-java@v3.5.1\n" + "      with:\n"
							+ "        java-version: ${{ env.JAVA_VERSION }}\n"
							+ "        distribution: adopt\n"
							+ "        cache: 'maven'\n"
							+ "    - name: Setup Graalvm\n"
							+ "      uses: DeLaGuardo/setup-graalvm@5.0\n"
							+ "      with:\n"
							+ "        graalvm: ${{ env.GRAALVM_VERSION }}\n"
							+ "        java: java${{ env.JAVA_VERSION }}\n"
							+ "    - name: Install native-image module\n"
							+ "      run: '& \"$env:JAVA_HOME\\bin\\gu\" install native-image'",
					"    - uses: graalvm/setup-graalvm@v1.0.7\n"
							+ "      with:\n"
							+ "        version: ${{ env.GRAALVM_VERSION }}\n"
							+ "        java-version: ${{ env.JAVA_VERSION }}\n"
							+ "        components: 'native-image'\n"
							+ "        github-token: ${{ secrets.GITHUB_TOKEN }}\n"
							+ "        cache: 'maven'"
			);
			FilesSilent.writeString(path, updated);
		});
	}

	private void useSpecificActionVersions(ChoreContext context) {
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
					"uses: actions/setup-node@v3\n",
					"uses: actions/setup-node@v3.5.1\n"
			);
			updated = updated.replace(
					"uses: actions/setup-java@v3\n",
					"uses: actions/setup-java@v3.5.1\n"
			);
			updated = updated.replaceAll(
					"github/codeql-action/init@v3.*",
					"github/codeql-action/init@v3"
			);
			updated = updated.replaceAll(
					"github/codeql-action/autobuild@v3.*",
					"github/codeql-action/autobuild@v3"
			);
			updated = updated.replaceAll(
					"github/codeql-action/analyze@v3.*",
					"github/codeql-action/analyze@v3"
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

	private void removeSetupJava370(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			String updated = FilesSilent.readString(path);
			updated = updated.replace(
					"uses: actions/setup-java@v3.7.0\n",
					"uses: actions/setup-java@v3.6.0\n"
			);
			FilesSilent.writeString(path, updated);
		});
	}

	private void replaceSetOutput(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			List<String> updated = FilesSilent.readAllLines(path)
					.stream()
					.map(s -> {
						if (s.trim().startsWith("echo \"::set-output name=")) {
							return s.replace("::set-output name=", "")
									.replace("::", "=") + " >> $GITHUB_OUTPUT";
						}
						return s;
					})
					.toList();
			FilesSilent.write(path, updated, "\n");
		});
	}

	private void updateGraalVmVersion(ChoreContext context) {
		Path main = context.resolve(".github/workflows/main.yaml");
		if (FilesSilent.exists(main)) {
			List<String> updated = FilesSilent.readAllLines(main)
					.stream()
					.map(s -> {
						if (s.startsWith("  GRAALVM_VERSION: 22.1.0")) {
							return s;
						}
						if (s.startsWith("  GRAALVM_VERSION: ")) {
							return "  GRAALVM_VERSION: 22.2.0";
						}
						return s;
					})
					.toList();
			FilesSilent.write(main, updated, "\n");
		}
	}

	private void removeCustomGithubPackagesMavenSettings(ChoreContext context) {
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
			FilesSilent.write(main, updated, "\n");
		}
	}

}
