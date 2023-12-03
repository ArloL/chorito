package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.RandomCronBuilder;
import io.github.arlol.chorito.tools.Renamer;

public class GitHubActionChore {

	private final ChoreContext context;
	private final RandomCronBuilder randomCronBuilder;

	public GitHubActionChore(ChoreContext context) {
		this.context = context;
		this.randomCronBuilder = new RandomCronBuilder(
				context.randomGenerator()
		);
	}

	public void doit() {
		ensureYamlFileExtension();
		updateChoresWorkflow();
		updateGraalVmVersion();
		removeCustomGithubPackagesMavenSettings();
		useSpecificActionVersions();
		replaceSetOutput();
		migrateToGraalSetupAction();
		migrateJavaDistributionFromAdoptToTemurin();
		updateCodeQlSchedule();
		updateMainSchedule();
		removeSetupJava370();
		migrateActionsCreateRelease();
		migrateActionsUploadReleaseAsset();
	}

	private void migrateActionsCreateRelease() {
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

	private void migrateActionsUploadReleaseAsset() {
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
		String randomDayOfMonth = randomCronBuilder.randomDayOfMonth();
		if (context.hasGitHubRemote()) {
			Path choresYaml = context.resolve(".github/workflows/chores.yaml");
			String currentChores = ClassPathFiles
					.readString("/workflows/chores.yaml");
			String cron;
			if (FilesSilent.exists(choresYaml)) {
				String content = FilesSilent.readString(choresYaml);
				cron = readCurrentCron(content).orElse(randomDayOfMonth);
				if (cron.equals("26 15 * * 5")) {
					cron = randomDayOfMonth;
				}
			} else {
				cron = randomDayOfMonth;
			}
			FilesSilent.writeString(
					choresYaml,
					currentChores.replace("1 6 16 * *", cron)
			);
		}
	}

	private void updateCodeQlSchedule() {
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

	private void updateMainSchedule() {
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

	private void migrateJavaDistributionFromAdoptToTemurin() {
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

	private void migrateToGraalSetupAction() {
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
					"uses: actions/setup-node@v3\n",
					"uses: actions/setup-node@v3.5.1\n"
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

	private void removeSetupJava370() {
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

	private void replaceSetOutput() {
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
			FilesSilent.write(main, updated, "\n");
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
			FilesSilent.write(main, updated, "\n");
		}
	}

}
