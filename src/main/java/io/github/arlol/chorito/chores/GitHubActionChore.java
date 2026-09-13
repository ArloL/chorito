package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.ExecutableFlagger;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.GitHubActionsWorkflowFile;
import io.github.arlol.chorito.tools.JavaVersions;
import io.github.arlol.chorito.tools.MyPaths;
import io.github.arlol.chorito.tools.RandomCronBuilder;
import io.github.arlol.chorito.tools.Template;

public class GitHubActionChore implements Chore {

	private static final String WORKFLOWS_DIRECTORY = ".github/workflows";
	private static final List<String> WORKFLOW_EXTENSIONS = List
			.of(".yaml", ".yml");
	private static final List<String> MAIN_WORKFLOWS = List
			.of(".github/workflows/main.yaml", ".github/workflows/main.yml");
	private static final String SETUP_GRAALVM_ACTION = "graalvm/setup-graalvm";
	private static final String DISTRIBUTION_TEMURIN = "distribution: temurin";
	private static final String VERSION_JOB = "version";
	private static final String VERSION_INPUT_PARAMETER = "version";
	private static final String DEBUG_JOB = "debug";

	/**
	 * One step of the chain, named so the sequence can be read and asserted.
	 */
	record Migration(
			String name,
			Consumer<ChoreContext> apply
	) {
	}

	/**
	 * One migration's dependency on an earlier one, and what it consumes.
	 * <p>
	 * Several migrations here match text that an earlier one produced, and
	 * before {@link #ORDERINGS} the only record of that was the order of the
	 * calls. Swapping two of them stops the later one matching -- no error, no
	 * failing test, just a repository that never gets migrated. Stated here,
	 * {@code GitHubActionChoreTest} enforces them.
	 */
	record Ordering(
			String first,
			String second,
			String because
	) {
	}

	static final List<Ordering> ORDERINGS = List.of(
			new Ordering(
					"useSpecificActionVersions",
					"migrateToGraalSetupAction",
					"the block it matches names actions/setup-java@v3.5.1, which exists only once bare v3 has been pinned"
			),
			new Ordering(
					"migrateToGraalSetupAction",
					"migrateJavaDistributionFromAdoptToTemurin",
					"the same block names 'distribution: adopt', which rewriting adopt to temurin destroys"
			),
			new Ordering(
					"migrateActionsCreateRelease",
					"migrateNcipoploReleaseAction",
					"it emits the ncipollo release block that normalising ncipollo versions then consumes"
			),
			new Ordering(
					"migrateEregonPublishRelease",
					"migrateNcipoploReleaseAction",
					"it emits a second ncipollo release block, at a pinned SHA, that the same normalisation consumes"
			)
	);

	/**
	 * Every migration, in the order it runs. The order is load bearing --
	 * {@link #ORDERINGS} says where and why -- and the set only grows, because
	 * nothing records which generation a repository has reached, so a migration
	 * cannot be proven dead and retired.
	 */
	List<Migration> migrations() {
		return List.of(
				new Migration(
						"updateChoresWorkflow",
						this::updateChoresWorkflow
				),
				new Migration(
						"updateGraalVmVersion",
						this::updateGraalVmVersion
				),
				new Migration(
						"removeCustomGithubPackagesMavenSettings",
						this::removeCustomGithubPackagesMavenSettings
				),
				new Migration(
						"useSpecificActionVersions",
						this::useSpecificActionVersions
				),
				new Migration("replaceSetOutput", this::replaceSetOutput),
				new Migration(
						"migrateToGraalSetupAction",
						this::migrateToGraalSetupAction
				),
				new Migration(
						"migrateJavaDistributionFromAdoptToTemurin",
						this::migrateJavaDistributionFromAdoptToTemurin
				),
				new Migration(
						"updateCodeQlSchedule",
						this::updateCodeQlSchedule
				),
				new Migration("updateMainSchedule", this::updateMainSchedule),
				new Migration("removeSetupJava370", this::removeSetupJava370),
				new Migration(
						"migrateActionsCreateRelease",
						this::migrateActionsCreateRelease
				),
				new Migration(
						"migrateActionsUploadReleaseAsset",
						this::migrateActionsUploadReleaseAsset
				),
				new Migration("updateGraalSteps", this::updateGraalSteps),
				new Migration("updateDebugSteps", this::updateDebugSteps),
				new Migration("updateVersionSteps", this::updateVersionSteps),
				new Migration("updatePermissions", this::updatePermissions),
				new Migration(
						"addCheckActionWorkflow",
						this::addCheckActionWorkflow
				),
				new Migration(
						"actionsCheckoutWithPersistCredentials",
						this::actionsCheckoutWithPersistCredentials
				),
				new Migration("quoteRedirects", this::quoteRedirects),
				new Migration("migrateZipProjects", this::migrateZipProjects),
				new Migration(
						"removeNeedsVersionOutputsChangelog",
						this::removeNeedsVersionOutputsChangelog
				),
				new Migration(
						"migrateEregonPublishRelease",
						this::migrateEregonPublishRelease
				),
				new Migration(
						"migrateNcipoploReleaseAction",
						this::migrateNcipoploReleaseAction
				),
				new Migration("migrateSetupGraalvm", this::migrateSetupGraalvm)
		);
	}

	@Override
	public ChoreContext doit(ChoreContext context) {
		migrations().forEach(migration -> migration.apply().accept(context));
		return context;
	}

	/**
	 * What each job of a main workflow cannot do its work without.
	 * <p>
	 * Stated here rather than read off the template because these are a
	 * requirement, not a copy: a job gets them whether or not chorito's own
	 * workflow happens to hold them today.
	 * {@link io.github.arlol.chorito.tools.Template Template} takes chorito's
	 * own extras off every workflow it hands out, and
	 * {@link AttestReleaseAssetsChore} grants those back where they are earned.
	 */
	private static final Map<String, Map<String, String>> REQUIRED_PERMISSIONS = Map
			.of(
					VERSION_JOB,
					Map.of("contents", "write"),
					"release",
					Map.of("contents", "write"),
					"deploy",
					Map.of("packages", "write")
			);

	void updatePermissions(ChoreContext context) {
		Optional<Path> mainWorkflow = mainWorkflow(context);
		if (mainWorkflow.isEmpty()) {
			return;
		}
		Path mainYaml = mainWorkflow.orElseThrow();
		var main = new GitHubActionsWorkflowFile(
				FilesSilent.readString(mainYaml)
		);
		REQUIRED_PERMISSIONS.forEach(
				(job, permissions) -> main.grantJobPermissions(job, permissions)
		);
		FilesSilent.writeString(mainYaml, main.asString());
	}

	void updateGraalSteps(ChoreContext context) {
		Optional<Path> mainWorkflow = mainWorkflow(context);
		if (mainWorkflow.isEmpty()) {
			return;
		}
		Path mainYaml = mainWorkflow.orElseThrow();
		String string = FilesSilent.readString(mainYaml);
		if (!string.contains("graalvm") || string.contains("gluonfx")) {
			return;
		}
		var main = new GitHubActionsWorkflowFile(string);
		if (!main.hasJob(VERSION_JOB)) {
			return;
		}

		var currentMain = Template.mainWorkflow();
		String before = main.asStringWithoutVersions();
		List<String> platformJobs = List.of("macos", "linux", "windows");
		platformJobs.forEach(job -> main.setJob(job, currentMain.getJob(job)));
		// The platform jobs upload their binaries so the release job can
		// download them again. A release that never downloads has nothing to
		// collect -- a library proving it works inside a native image rather
		// than shipping one -- so the upload is plumbing to nowhere. Keying on
		// the download rather than on what the release publishes keeps the
		// older release jobs, which attach assets from their own paths.
		if (!main.hasStepUsing("release", "actions/download-artifact")) {
			platformJobs.forEach(job -> {
				main.removeStepByName(job, "Move artifacts");
				main.removeStepUsing(job, "actions/upload-artifact");
			});
		}
		String after = main.asStringWithoutVersions();
		if (!after.equals(before)) {
			FilesSilent.writeString(mainYaml, main.asString());
		}
	}

	void updateDebugSteps(ChoreContext context) {
		var currentMain = Template.choresWorkflow();
		var debugJob = currentMain.getJob(DEBUG_JOB);
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			var workflow = new GitHubActionsWorkflowFile(
					FilesSilent.readString(path)
			);
			if (workflow.hasJob(DEBUG_JOB)) {
				String before = workflow.asStringWithoutVersions();
				workflow.setJob(DEBUG_JOB, debugJob);
				String after = workflow.asStringWithoutVersions();
				if (!after.equals(before)) {
					FilesSilent.writeString(path, workflow.asString());
				}
			}
		});
	}

	void updateVersionSteps(ChoreContext context) {
		var currentMain = Template.mainWorkflow();
		var versionJob = currentMain.getJob(VERSION_JOB);
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			var workflow = new GitHubActionsWorkflowFile(
					FilesSilent.readString(path)
			);
			if (workflow.hasJob(VERSION_JOB)) {
				String before = workflow.asStringWithoutVersions();
				workflow.setJob(VERSION_JOB, versionJob);
				String after = workflow.asStringWithoutVersions();
				if (!after.equals(before)) {
					FilesSilent.writeString(path, workflow.asString());
				}
			}
		});
	}

	void migrateActionsCreateRelease(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
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

	void migrateActionsUploadReleaseAsset(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
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

	void updateChoresWorkflow(ChoreContext context) {
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

			var templateWorkflow = Template.choresWorkflow();

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

	void updateCodeQlSchedule(ChoreContext context) {
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

	void updateMainSchedule(ChoreContext context) {
		RandomCronBuilder randomCronBuilder = new RandomCronBuilder(
				context.randomGenerator()
		);
		String randomDayOfMonth = randomCronBuilder.randomDayOfMonth();
		mainWorkflow(context).ifPresent(yaml -> {
			String content = FilesSilent.readString(yaml);
			readCurrentCron(content).ifPresent(currentCron -> {
				if (currentCron.equals("17 4 5 * *")) {
					FilesSilent.writeString(
							yaml,
							content.replace(currentCron, randomDayOfMonth)
					);
				}
			});
		});
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

	void migrateJavaDistributionFromAdoptToTemurin(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			String updated = FilesSilent.readString(path);
			updated = updated
					.replace("distribution: adopt", DISTRIBUTION_TEMURIN);
			updated = updated
					.replace("distribution: 'adopt'", DISTRIBUTION_TEMURIN);
			updated = updated
					.replace("distribution: \"adopt\"", DISTRIBUTION_TEMURIN);
			FilesSilent.writeString(path, updated);
		});
	}

	void migrateToGraalSetupAction(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
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

	void useSpecificActionVersions(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
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

	void removeSetupJava370(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			String updated = FilesSilent.readString(path);
			updated = updated.replace(
					"uses: actions/setup-java@v3.7.0\n",
					"uses: actions/setup-java@v3.6.0\n"
			);
			FilesSilent.writeString(path, updated);
		});
	}

	void replaceSetOutput(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
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

	void updateGraalVmVersion(ChoreContext context) {
		mainWorkflow(context).ifPresent(main -> {
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
		});
	}

	void removeCustomGithubPackagesMavenSettings(ChoreContext context) {
		mainWorkflow(context).ifPresent(main -> {
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
		});
	}

	void addCheckActionWorkflow(ChoreContext context) {
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

			var templateWorkflow = Template.checkActionsWorkflow();

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

	void actionsCheckoutWithPersistCredentials(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
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

	void quoteRedirects(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			var yaml = FilesSilent.readString(path);
			yaml = yaml.replace("> $GITHUB_ENV", "> \"${GITHUB_ENV}\"");
			yaml = yaml.replace("> $GITHUB_OUTPUT", "> \"${GITHUB_OUTPUT}\"");
			yaml = yaml.replace("> \"$GITHUB_ENV\"", "> \"${GITHUB_ENV}\"");
			yaml = yaml
					.replace("> \"$GITHUB_OUTPUT\"", "> \"${GITHUB_OUTPUT}\"");
			FilesSilent.writeString(path, yaml);
		});
	}

	void migrateZipProjects(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
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

	void removeNeedsVersionOutputsChangelog(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			String updated = FilesSilent.readString(path);
			String target = """
					        body: ${{ needs.version.outputs.changelog }}
					""";
			updated = updated.replace(target, "");
			FilesSilent.writeString(path, updated);
		});
	}

	void migrateEregonPublishRelease(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
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

	void migrateNcipoploReleaseAction(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			String current = FilesSilent.readString(path);
			if (!current.contains("id: create_release")) {
				return;
			}
			var updated = current.replaceAll(
					"(ncipollo/release-action|shogo82148/actions-upload-release-asset)@[^\\n]+",
					"$1@"
			);
			if (updated
					.contains("asset_content_type: application/x-executable")) {
				var target = """
						    - name: Create Release
						      id: create_release
						      uses: ncipollo/release-action@
						      with:
						        draft: true
						        name: Release ${{ needs.version.outputs.new_version }}
						        tag: v${{ needs.version.outputs.new_version }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-linux-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/linux.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@
						      with:
						        asset_content_type: application/x-executable
						        asset_name: ${{ env.ARTIFACT }}-linux
						        asset_path: ./target/${{ env.ARTIFACT }}-linux-${{ needs.version.outputs.new_version }}/${{ env.ARTIFACT }}-linux-${{ needs.version.outputs.new_version }}
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-windows-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/windows.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@
						      with:
						        asset_content_type: application/vnd.microsoft.portable-executable
						        asset_name: ${{ env.ARTIFACT }}-windows.exe
						        asset_path: ./target/${{ env.ARTIFACT }}-windows-${{ needs.version.outputs.new_version }}/${{ env.ARTIFACT }}-windows-${{ needs.version.outputs.new_version }}.exe
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-macos-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/macos.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@
						      with:
						        asset_content_type: application/octet-stream
						        asset_name: ${{ env.ARTIFACT }}-macos
						        asset_path: ./target/${{ env.ARTIFACT }}-macos-${{ needs.version.outputs.new_version }}/${{ env.ARTIFACT }}-macos-${{ needs.version.outputs.new_version }}
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - uses: ncipollo/release-action@
						      with:
						        allowUpdates: true
						        immutableCreate: true
						        omitBodyDuringUpdate: true
						        omitNameDuringUpdate: true
						        tag: v${{ needs.version.outputs.new_version }}
						        updateOnlyUnreleased: true
						""";
				var replacement = """
						    - name: Create Release
						      env:
						        GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
						        NEW_VERSION: ${{ needs.version.outputs.new_version }}
						      run: |
						        gh release create "v${NEW_VERSION}" \\
						          --title "Release ${NEW_VERSION}" \\
						          "./target/linux.zip#${ARTIFACT}-linux-${NEW_VERSION}.zip" \\
						          "./target/${ARTIFACT}-linux-${NEW_VERSION}/${ARTIFACT}-linux-${NEW_VERSION}#${ARTIFACT}-linux" \\
						          "./target/windows.zip#${ARTIFACT}-windows-${NEW_VERSION}.zip" \\
						          "./target/${ARTIFACT}-windows-${NEW_VERSION}/${ARTIFACT}-windows-${NEW_VERSION}.exe#${ARTIFACT}-windows.exe" \\
						          "./target/macos.zip#${ARTIFACT}-macos-${NEW_VERSION}.zip" \\
						          "./target/${ARTIFACT}-macos-${NEW_VERSION}/${ARTIFACT}-macos-${NEW_VERSION}#${ARTIFACT}-macos"
						""";
				updated = updated.replace(target, replacement);
			} else if (updated.contains("allowUpdates: true")) {
				var target = """
						    - name: Create Release
						      id: create_release
						      uses: ncipollo/release-action@
						      with:
						        draft: true
						        name: Release ${{ needs.version.outputs.new_version }}
						        tag: v${{ needs.version.outputs.new_version }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-linux-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/linux.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-windows-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/windows.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - name: Upload Release Asset
						      uses: shogo82148/actions-upload-release-asset@
						      with:
						        asset_content_type: application/zip
						        asset_name: ${{ env.ARTIFACT }}-macos-${{ needs.version.outputs.new_version }}.zip
						        asset_path: ./target/macos.zip
						        upload_url: ${{ steps.create_release.outputs.upload_url }}
						    - uses: ncipollo/release-action@
						      with:
						        allowUpdates: true
						        immutableCreate: true
						        omitBodyDuringUpdate: true
						        omitNameDuringUpdate: true
						        tag: v${{ needs.version.outputs.new_version }}
						        updateOnlyUnreleased: true
						""";
				var replacement = """
						    - name: Create Release
						      env:
						        GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
						        NEW_VERSION: ${{ needs.version.outputs.new_version }}
						      run: |
						        gh release create "v${NEW_VERSION}" \\
						          --title "Release ${NEW_VERSION}" \\
						          "./target/linux.zip#${ARTIFACT}-linux-${NEW_VERSION}.zip" \\
						          "./target/windows.zip#${ARTIFACT}-windows-${NEW_VERSION}.zip" \\
						          "./target/macos.zip#${ARTIFACT}-macos-${NEW_VERSION}.zip"
						""";
				updated = updated.replace(target, replacement);
			} else {
				return;
			}
			FilesSilent.writeString(path, updated);
		});
	}

	void migrateSetupGraalvm(ChoreContext context) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			String current = FilesSilent.readString(path);
			var workflow = new GitHubActionsWorkflowFile(current);
			String before = workflow.asStringWithoutVersions();

			workflow.removeEnv("GRAALVM_VERSION");
			workflow.removeEnv("JAVA_VERSION");

			workflow.removeInputParameterFromAction(
					SETUP_GRAALVM_ACTION,
					"github-token"
			);
			workflow.removeInputParameterFromAction(
					SETUP_GRAALVM_ACTION,
					VERSION_INPUT_PARAMETER
			);
			workflow.removeInputParameterFromAction(
					SETUP_GRAALVM_ACTION,
					"components"
			);
			workflow.replaceActionWith(
					SETUP_GRAALVM_ACTION,
					"actions/setup-java@dded0888837ed1f317902acf8a20df0ad188d165",
					"v5.0.0"
			);

			var pinnedJavaVersion = workflow.getPinnedJavaVersion();
			workflow.removeInputParameterFromAction(
					"actions/setup-java",
					"java-version"
			);
			workflow.addInputParameterToAction(
					"actions/setup-java",
					"java-version-file",
					".tool-versions"
			);
			// A job asking for Temurin here builds no native image, and
			// .tool-versions would hand it a GraalVM anyway, so it pins a
			// version of its own that Renovate owns.
			if (JavaVersions.buildsOnGraalVm(context)) {
				workflow.pinTemurinJavaVersion(
						pinnedJavaVersion.orElse(JavaVersions.TEMURIN)
				);
			}

			workflow.singleToDoubleQuote();

			String after = workflow.asStringWithoutVersions();
			if (!after.equals(before)) {
				FilesSilent.writeString(path, workflow.asString());
			}
		});
	}

	/**
	 * The main workflow, preferring {@code main.yaml} over {@code main.yml}
	 * when both exist.
	 */
	private static Optional<Path> mainWorkflow(ChoreContext context) {
		return MAIN_WORKFLOWS.stream()
				.map(context::resolve)
				.filter(FilesSilent::exists)
				.findFirst();
	}

}
