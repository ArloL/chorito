package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.GitHubActionsWorkflowFile;
import io.github.arlol.chorito.tools.JavaVersions;
import io.github.arlol.chorito.tools.MyPaths;
import io.github.arlol.chorito.tools.RandomCronBuilder;
import io.github.arlol.chorito.tools.Template;
import io.github.arlol.chorito.tools.WorkflowJobs;

public class GitHubActionChore implements Chore {

	private static final String WORKFLOWS_DIRECTORY = ".github/workflows";
	private static final List<String> WORKFLOW_EXTENSIONS = List
			.of(".yaml", ".yml");
	private static final List<String> MAIN_WORKFLOWS = List
			.of(".github/workflows/main.yaml", ".github/workflows/main.yml");
	private static final String DISTRIBUTION_TEMURIN = "distribution: temurin";
	private static final List<String> TRUNK_BRANCHES = List
			.of("main", "master");
	private static final String DEPENDABOT_CONDITION = " && !startsWith(github.ref, 'refs/heads/dependabot/')";

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
					"migrateActionsCreateRelease",
					"migrateNcipoploReleaseAction",
					"it emits the ncipollo release block that normalising ncipollo versions then consumes"
			),
			new Ordering(
					"migrateEregonPublishRelease",
					"migrateNcipoploReleaseAction",
					"it emits a second ncipollo release block, at a pinned SHA, that the same normalisation consumes"
			),
			new Ordering(
					"updateVersionSteps",
					"narrowBranchConditions",
					"it copies the version job from chorito's own main.yaml, whose ref condition names chorito's branch, and only narrowing afterwards points that copy at the branch the repository really has"
			),
			new Ordering(
					"updateMainTriggers",
					"removeDeadDependabotCondition",
					"the dependabot exclusion stays live until push is filtered to one branch, and this is the migration that filters it"
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
						"removeCustomGithubPackagesMavenSettings",
						this::removeCustomGithubPackagesMavenSettings
				),
				new Migration(
						"useSpecificActionVersions",
						this::useSpecificActionVersions
				),
				new Migration("replaceSetOutput", this::replaceSetOutput),
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
				new Migration(
						"useToolVersionsForSetupJava",
						this::useToolVersionsForSetupJava
				),
				new Migration("updateMainTriggers", this::updateMainTriggers),
				new Migration(
						"removeDeadDependabotCondition",
						this::removeDeadDependabotCondition
				),
				new Migration(
						"narrowBranchConditions",
						this::narrowBranchConditions
				)
		);
	}

	@Override
	public ChoreContext doit(ChoreContext context) {
		migrations().forEach(migration -> migration.apply().accept(context));
		return context;
	}

	void updatePermissions(ChoreContext context) {
		Optional<Path> mainWorkflow = mainWorkflow(context);
		if (mainWorkflow.isEmpty()) {
			return;
		}
		Path mainYaml = mainWorkflow.orElseThrow();
		var main = new GitHubActionsWorkflowFile(
				FilesSilent.readString(mainYaml)
		);
		WorkflowJobs.REQUIRED_PERMISSIONS.forEach(
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
		if (!main.hasJob(WorkflowJobs.VERSION)) {
			return;
		}

		var currentMain = Template.mainWorkflow();
		String before = main.asStringWithoutVersions();
		WorkflowJobs.PLATFORMS
				.forEach(job -> main.setJob(job, currentMain.getJob(job)));
		// The platform jobs upload their binaries so the release job can
		// download them again. A release that never downloads has nothing to
		// collect -- a library proving it works inside a native image rather
		// than shipping one -- so the upload is plumbing to nowhere. Keying on
		// the download rather than on what the release publishes keeps the
		// older release jobs, which attach assets from their own paths.
		if (!main.hasStepUsing(
				WorkflowJobs.RELEASE,
				"actions/download-artifact"
		)) {
			WorkflowJobs.PLATFORMS.forEach(job -> {
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
		var debugJob = Template.choresWorkflow().getJob(WorkflowJobs.DEBUG);
		GitHubActionsWorkflowFile.updateEach(context, workflow -> {
			if (workflow.hasJob(WorkflowJobs.DEBUG)) {
				workflow.setJob(WorkflowJobs.DEBUG, debugJob);
			}
		});
	}

	/**
	 * Skipped when the branch is unknown, because the job it copies carries a
	 * {@code github.ref} condition naming chorito's own branch.
	 * {@code narrowBranchConditions} repoints that copy afterwards, and without
	 * a branch to repoint it to a repository on {@code master} would be handed
	 * a version job that can never tag -- a release that quietly stops
	 * happening, with no failed run to show for it.
	 */
	void updateVersionSteps(ChoreContext context) {
		if (context.mainBranch().isEmpty()) {
			return;
		}
		var versionJob = Template.mainWorkflow().getJob(WorkflowJobs.VERSION);
		GitHubActionsWorkflowFile.updateEach(context, workflow -> {
			if (workflow.hasJob(WorkflowJobs.VERSION)) {
				workflow.setJob(WorkflowJobs.VERSION, versionJob);
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
			// CodeQlAnalysisChore writes this file and carries the repository's
			// existing schedule across, so this only converts the fixed weekly
			// crons the older workflows shipped with. Asking whether the cron
			// is already randomised is what keeps the two chores off each
			// other: rewrite on anything else and they would pick a new time
			// on every run.
			if (!RandomCronBuilder.isRandomDayOfMonth(currentCron)) {
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
			// The template says main because chorito does. A repository on
			// master needs the same file with the name swapped, or it gets a
			// workflow whose triggers can never match.
			context.mainBranch().ifPresent(templateWorkflow::renameOnBranches);

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

	void useToolVersionsForSetupJava(ChoreContext context) {
		GitHubActionsWorkflowFile.updateEach(context, workflow -> {
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

	/**
	 * Narrows main.yaml to the trunk branch and the pull requests aimed at it.
	 * <p>
	 * The shape chorito shipped was a bare {@code push:} -- every branch, every
	 * time -- with {@code pull_request} cut down to {@code reopened}. That
	 * covers a branch pushed to this repository and misses a fork completely: a
	 * fork's push never reaches here, and without {@code opened} and
	 * {@code synchronize} a fork's pull request builds only if somebody closes
	 * and reopens it. Filtering push to the trunk and letting pull_request
	 * cover everything else closes that, and hands fork pull requests to the
	 * approval gate at the same time.
	 */
	void updateMainTriggers(ChoreContext context) {
		Optional<String> branch = context.mainBranch();
		if (branch.isEmpty()) {
			return;
		}
		mainWorkflow(context).ifPresent(yaml -> {
			var main = new GitHubActionsWorkflowFile(
					FilesSilent.readString(yaml)
			);
			if (!main.hasBarePush()) {
				return;
			}
			main.setOnPushAndPullRequestBranches(branch.orElseThrow());
			FilesSilent.writeString(yaml, main.asString());
		});
	}

	/**
	 * Drops the deploy job's dependabot exclusion once push is filtered.
	 * <p>
	 * The job runs {@code if github.event_name == 'push' && !startsWith(...)},
	 * and dependabot's branches were worth excluding while push fired for every
	 * branch. Filtered to the trunk, {@code github.ref} on a push can only be
	 * that branch, so the second half can never be false. A condition that
	 * reads as though it still decides something is worse than no condition, so
	 * it goes -- but only for a workflow whose push really is filtered.
	 */
	void removeDeadDependabotCondition(ChoreContext context) {
		mainWorkflow(context).ifPresent(yaml -> {
			String content = FilesSilent.readString(yaml);
			var main = new GitHubActionsWorkflowFile(content);
			if (!main.hasOnPushBranches()) {
				return;
			}
			String updated = content.replace(DEPENDABOT_CONDITION, "");
			if (!updated.equals(content)) {
				FilesSilent.writeString(yaml, updated);
			}
		});
	}

	/**
	 * Points {@code github.ref} conditions at the branch the repository really
	 * has.
	 * <p>
	 * The workflows say {@code refs/heads/master || refs/heads/main} because
	 * one file had to serve both kinds of repository. Now that
	 * {@link ChoreContext#mainBranch()} can tell them apart, each repository
	 * gets the one name that is true for it, and the repositories that have
	 * already renamed stop carrying a reference to a branch they no longer
	 * have.
	 * <p>
	 * A name is only repointed when that branch is absent. A repository keeping
	 * both {@code main} and {@code master} alive still means something by a
	 * condition naming {@code master}, and rewriting it would change which
	 * pushes release.
	 */
	void narrowBranchConditions(ChoreContext context) {
		Optional<String> mainBranch = context.mainBranch();
		if (mainBranch.isEmpty()) {
			return;
		}
		String kept = branchCondition(mainBranch.orElseThrow());
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			String content = FilesSilent.readString(path);
			String updated = content
					.replace(
							branchCondition("master") + " || "
									+ branchCondition("main"),
							kept
					)
					.replace(
							branchCondition("main") + " || "
									+ branchCondition("master"),
							kept
					);
			for (String absent : TRUNK_BRANCHES) {
				if (!context.branches().contains(absent)) {
					updated = updated.replace(branchCondition(absent), kept);
				}
			}
			if (!updated.equals(content)) {
				FilesSilent.writeString(path, updated);
			}
		});
	}

	private static String branchCondition(String branch) {
		return "github.ref == 'refs/heads/" + branch + "'";
	}

}
