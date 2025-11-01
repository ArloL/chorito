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
		addCheckActionWorkflow(context);
		actionsCheckoutWithPersistCredentials(context);
		quoteRedirects(context);
		migrateZipProjects(context);
		removeNeedsVersionOutputsChangelog(context);
		migrateEregonPublishRelease(context);
		migrateSetupGraalvm(context);
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
		if (!string.contains("graalvm") || string.contains("gluonfx")) {
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
				templateWorkflow.setOnScheduleCron(randomDayOfMonth);
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
			updated = updated.replace("""

					    - name: Set up Visual Studio shell
					      uses: egor-tensin/vs-shell@v2\
					""", "");
			updated = updated.replace("""
					    - uses: actions/setup-java@v3.5.1
					      with:
					        java-version: ${{ env.JAVA_VERSION }}
					        distribution: adopt
					        cache: 'maven'
					    - name: Setup Graalvm
					      uses: DeLaGuardo/setup-graalvm@5.0
					      with:
					        graalvm: ${{ env.GRAALVM_VERSION }}
					        java: java${{ env.JAVA_VERSION }}
					    - name: Install native-image module
					      run: gu install native-image\
					""", """
					    - uses: graalvm/setup-graalvm@v1.0.7
					      with:
					        version: ${{ env.GRAALVM_VERSION }}
					        java-version: ${{ env.JAVA_VERSION }}
					        components: 'native-image'
					        github-token: ${{ secrets.GITHUB_TOKEN }}
					        cache: 'maven'\
					""");
			updated = updated.replace(
					"""
							    - uses: actions/setup-java@v3.5.1
							      with:
							        java-version: ${{ env.JAVA_VERSION }}
							        distribution: adopt
							        cache: 'maven'
							    - name: Setup Graalvm
							      uses: DeLaGuardo/setup-graalvm@5.0
							      with:
							        graalvm: ${{ env.GRAALVM_VERSION }}
							        java: java${{ env.JAVA_VERSION }}
							    - name: Install native-image module
							      run: '& "$env:JAVA_HOME\\bin\\gu" install native-image'\
							""",
					"""
							    - uses: graalvm/setup-graalvm@v1.0.7
							      with:
							        version: ${{ env.GRAALVM_VERSION }}
							        java-version: ${{ env.JAVA_VERSION }}
							        components: 'native-image'
							        github-token: ${{ secrets.GITHUB_TOKEN }}
							        cache: 'maven'\
							"""
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
									.replace("::", "=")
									+ " >> \"${GITHUB_OUTPUT}\"";
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

	private void addCheckActionWorkflow(ChoreContext context) {
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
			Path checkActionsYaml = context
					.resolve(".github/workflows/check-actions.yaml");

			var templateWorkflow = new GitHubActionsWorkflowFile(
					ClassPathFiles.readString(
							"github-settings/workflows/check-actions.yaml"
					)
			);

			GitHubActionsWorkflowFile checkActionsWorkflow;
			if (FilesSilent.exists(checkActionsYaml)) {
				checkActionsWorkflow = new GitHubActionsWorkflowFile(
						FilesSilent.readString(checkActionsYaml)
				);
				templateWorkflow.setOnScheduleCron(
						checkActionsWorkflow.getOnScheduleCron().orElseThrow()
				);
			} else {
				checkActionsWorkflow = templateWorkflow.copy();
				templateWorkflow.setOnScheduleCron(randomDayOfMonth);
			}

			if (!templateWorkflow.asStringWithoutVersions()
					.equals(checkActionsWorkflow.asStringWithoutVersions())) {
				FilesSilent.writeString(
						checkActionsYaml,
						templateWorkflow.asString()
				);
			}
		}
	}

	private void actionsCheckoutWithPersistCredentials(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			String input = FilesSilent.readString(path);
			var checkActionsWorkflow = new GitHubActionsWorkflowFile(input);
			checkActionsWorkflow.clearPermissions();
			checkActionsWorkflow.actionsCheckoutWithPersistCredentials();
			checkActionsWorkflow.sortKeys();
			if (!input.equals(checkActionsWorkflow.asStringWithoutVersions())) {
				FilesSilent.writeString(path, checkActionsWorkflow.asString());
			}
		});
	}

	private void quoteRedirects(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			var yaml = FilesSilent.readString(path);
			yaml = yaml.replace("> $GITHUB_ENV", "> \"${GITHUB_ENV}\"");
			yaml = yaml.replace("> $GITHUB_OUTPUT", "> \"${GITHUB_OUTPUT}\"");
			yaml = yaml.replace("> \"$GITHUB_ENV\"", "> \"${GITHUB_ENV}\"");
			yaml = yaml
					.replace("> \"$GITHUB_OUTPUT\"", "> \"${GITHUB_OUTPUT}\"");
			FilesSilent.writeString(path, yaml);
		});
	}

	private void migrateZipProjects(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			String updated = FilesSilent.readString(path);
			String target = """
					    - name: Build project
					      working-directory: target
					      run: |
					        zip -r windows.zip ${{ env.ARTIFACT }}-windows-${{ needs.version.outputs.new_version }}/
					        zip -r linux.zip ${{ env.ARTIFACT }}-linux-${{ needs.version.outputs.new_version }}/
					        zip -r macos.zip ${{ env.ARTIFACT }}-macos-${{ needs.version.outputs.new_version }}/
					""";
			String replacement = """
					    - name: Zip artifacts
					      working-directory: target
					      env:
					        NEW_VERSION: ${{ needs.version.outputs.new_version }}
					      run: |
					        zip -r windows.zip "${ARTIFACT}-windows-${NEW_VERSION}/"
					        zip -r linux.zip "${ARTIFACT}-linux-${NEW_VERSION}/"
					        zip -r macos.zip "${ARTIFACT}-macos-${NEW_VERSION}/"
					""";
			updated = updated.replace(target, replacement);
			FilesSilent.writeString(path, updated);
		});
	}

	private void removeNeedsVersionOutputsChangelog(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			String updated = FilesSilent.readString(path);
			String target = """
					        body: ${{ needs.version.outputs.changelog }}
					""";
			updated = updated.replace(target, "");
			FilesSilent.writeString(path, updated);
		});
	}

	private void migrateEregonPublishRelease(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			var current = FilesSilent.readString(path);

			var target = """
					    - uses: eregon/publish-release@01df127f5e9a3c26935118e22e738d95b59d10ce # v1.0.6
					      env:
					        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
					      with:
					        release_id: ${{ steps.create_release.outputs.id }}
					""";
			var replacement = """
					    - uses: ncipollo/release-action@b7eabc95ff50cbeeedec83973935c8f306dfcd0b # v1.20.0
					      with:
					        tag: v${{ needs.version.outputs.new_version }}
					        allowUpdates: true
					        immutableCreate: true
					        omitBodyDuringUpdate: true
					        omitNameDuringUpdate: true
					        updateOnlyUnreleased: true
					""";
			var updated = current.replaceAll("eregon/publish-release@.*$", "");
			updated = updated.replace(target, replacement);
			FilesSilent.writeString(path, updated);
		});
	}

	private void migrateSetupGraalvm(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			String current = FilesSilent.readString(path);
			var workflow = new GitHubActionsWorkflowFile(current);
			String before = workflow.asStringWithoutVersions();

			workflow.removeEnv("GRAALVM_VERSION");
			workflow.removeEnv("JAVA_VERSION");

			workflow.removeInputParameterFromAction(
					"graalvm/setup-graalvm",
					"github-token"
			);
			workflow.removeInputParameterFromAction(
					"graalvm/setup-graalvm",
					"version"
			);
			workflow.removeInputParameterFromAction(
					"graalvm/setup-graalvm",
					"components"
			);
			workflow.replaceActionWith(
					"graalvm/setup-graalvm",
					"actions/setup-java@dded0888837ed1f317902acf8a20df0ad188d165",
					"v5.0.0"
			);

			workflow.removeInputParameterFromAction(
					"actions/setup-java",
					"java-version"
			);
			workflow.addInputParameterToAction(
					"actions/setup-java",
					"java-version-file",
					".tool-versions"
			);

			workflow.singleToDoubleQuote();

			String after = workflow.asStringWithoutVersions();
			if (!after.equals(before)) {
				FilesSilent.writeString(path, workflow.asString());
			}
		});
	}

}
