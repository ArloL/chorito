package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class RenovateChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private ChoreContext githubContext() {
		return extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.build();
	}

	@Test
	public void testWithNothing() {
		new RenovateChore().doit(extension.choreContext());

		assertThat(extension.relativePaths()).isEmpty();
	}

	@Test
	public void testRenamesRenovateJsonToRenovateJson5() throws Exception {
		Path renovateJson = extension.root().resolve("renovate.json");
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(
				renovateJson,
				"""
						{
						    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
						    "extends": [
						        "config:recommended",
						    ],
						    "labels": [
						        "dependencies",
						    ],
						    "addLabels": [
						        "{{manager}}",
						    ],
						    "minimumReleaseAge": "7 days",
						    "prConcurrentLimit": 10,
						    "prHourlyLimit": 5,
						    "schedule": [
						        "* * 20-23 * *",
						    ],
						    "vulnerabilityAlerts": {
						        "schedule": [
						            "at any time",
						        ],
						        "minimumReleaseAge": "0 days",
						        "addLabels": [
						            "security",
						        ],
						    },
						}
						"""
		);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson).doesNotExist();
		assertThat(renovateJson5).exists();
	}

	@Test
	public void testCreatesRenovateJson5ForGitHubRepo() throws Exception {
		new RenovateChore().doit(githubContext());

		Path renovateJson5 = extension.root().resolve("renovate.json5");
		assertThat(renovateJson5).content()
				.isEqualTo(
						"""
								{
								    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
								    "addLabels": [
								        "{{manager}}",
								    ],
								    "extends": [
								        "config:recommended",
								    ],
								    "labels": [
								        "dependencies",
								    ],
								    "minimumReleaseAge": "7 days",
								    "prConcurrentLimit": 10,
								    "prHourlyLimit": 5,
								    "schedule": [
								        "* * 20-23 * *",
								    ],
								    "vulnerabilityAlerts": {
								        "addLabels": [
								            "security",
								        ],
								        "minimumReleaseAge": "0 days",
								        "schedule": [
								            "at any time",
								        ],
								    },
								}
								"""
				);
	}

	@Test
	public void testDoesNotCreateRenovateJson5ForNonGitHubRepo()
			throws Exception {
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://gitlab.com/example/example"))
				.build();

		new RenovateChore().doit(context);

		assertThat(extension.root().resolve("renovate.json")).doesNotExist();
		assertThat(extension.root().resolve("renovate.json5")).doesNotExist();
	}

	@Test
	public void testDoesNotCreateRenovateJson5WithNoRemotes() throws Exception {
		new RenovateChore().doit(extension.choreContext());

		assertThat(extension.root().resolve("renovate.json")).doesNotExist();
		assertThat(extension.root().resolve("renovate.json5")).doesNotExist();
	}

	@Test
	public void testUpdatesMinimumReleaseAgeFrom4DaysTo7Days()
			throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(
				renovateJson5,
				"""
						{
						  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
						  "extends": [
						    "config:recommended"
						  ],
						  "labels": ["dependencies"],
						  "addLabels": ["{{manager}}"],
						  "minimumReleaseAge": "4 days",
						  "prConcurrentLimit": 10,
						  "prHourlyLimit": 5,
						  "schedule": ["* * 20-23 * *"],
						  "vulnerabilityAlerts": {
						    "schedule": ["at any time"],
						    "minimumReleaseAge": "0 days",
						    "addLabels": ["security"]
						  }
						}
						"""
		);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content()
				.isEqualTo(
						"""
								{
								    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
								    "addLabels": [
								        "{{manager}}",
								    ],
								    "extends": [
								        "config:recommended",
								    ],
								    "labels": [
								        "dependencies",
								    ],
								    "minimumReleaseAge": "7 days",
								    "prConcurrentLimit": 10,
								    "prHourlyLimit": 5,
								    "schedule": [
								        "* * 20-23 * *",
								    ],
								    "vulnerabilityAlerts": {
								        "addLabels": [
								            "security",
								        ],
								        "minimumReleaseAge": "0 days",
								        "schedule": [
								            "at any time",
								        ],
								    },
								}
								"""
				);
	}

	@Test
	public void testWidensTheMonthlyWindow() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(renovateJson5, """
				{
				  "prConcurrentLimit": 10,
				  "prHourlyLimit": 5,
				  "schedule": ["on the 20th day of the month"]
				}
				""");

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content().contains("""
				    "schedule": [
				        "* * 20-23 * *",
				    ],
				""");
	}

	@Test
	public void testKeepsACustomSchedule() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(renovateJson5, """
				{
				  "prConcurrentLimit": 10,
				  "prHourlyLimit": 5,
				  "schedule": ["on the 20th day of the month", "every weekend"]
				}
				""");

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content()
				.contains("on the 20th day of the month")
				.doesNotContain("20-23");
	}

	@Test
	public void testAddsLabelsWhenMissing() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(
				renovateJson5,
				"""
						{
						  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
						  "extends": [
						    "config:recommended"
						  ],
						  "minimumReleaseAge": "7 days",
						  "prConcurrentLimit": 10,
						  "prHourlyLimit": 5,
						}
						"""
		);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content()
				.isEqualTo(
						"""
								{
								    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
								    "addLabels": [
								        "{{manager}}",
								    ],
								    "extends": [
								        "config:recommended",
								    ],
								    "labels": [
								        "dependencies",
								    ],
								    "minimumReleaseAge": "7 days",
								    "prConcurrentLimit": 10,
								    "prHourlyLimit": 5,
								}
								"""
				);
	}

	@Test
	public void testAddsSecurityLabelToVulnerabilityAlerts() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(
				renovateJson5,
				"""
						{
						  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
						  "extends": [
						    "config:recommended"
						  ],
						  "labels": ["dependencies"],
						  "addLabels": ["{{manager}}"],
						  "minimumReleaseAge": "7 days",
						  "prConcurrentLimit": 10,
						  "prHourlyLimit": 5,
						  "vulnerabilityAlerts": {
						    "schedule": ["at any time"],
						    "minimumReleaseAge": "0 days"
						  }
						}
						"""
		);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content()
				.isEqualTo(
						"""
								{
								    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
								    "addLabels": [
								        "{{manager}}",
								    ],
								    "extends": [
								        "config:recommended",
								    ],
								    "labels": [
								        "dependencies",
								    ],
								    "minimumReleaseAge": "7 days",
								    "prConcurrentLimit": 10,
								    "prHourlyLimit": 5,
								    "vulnerabilityAlerts": {
								        "addLabels": [
								            "security",
								        ],
								        "minimumReleaseAge": "0 days",
								        "schedule": [
								            "at any time",
								        ],
								    },
								}
								"""
				);
	}

	@Test
	public void testDoesNotModifyAlreadyUpToDateFile() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		String content = """
				{
				    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "extends": [
				        "config:recommended",
				    ],
				    "labels": [
				        "dependencies",
				    ],
				    "minimumReleaseAge": "7 days",
				    "prConcurrentLimit": 10,
				    "prHourlyLimit": 5,
				    "schedule": [
				        "* * 20-23 * *",
				    ],
				    "vulnerabilityAlerts": {
				        "addLabels": [
				            "security",
				        ],
				        "minimumReleaseAge": "0 days",
				        "schedule": [
				            "at any time",
				        ],
				    },
				}
				""";
		FilesSilent.writeString(renovateJson5, content);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content().isEqualTo(content);
	}

	@Test
	public void testDoesNotAddLabelsWhenAlreadyPresent() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		String content = """
				{
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "labels": [
				        "custom",
				    ],
				    "minimumReleaseAge": "7 days",
				    "prConcurrentLimit": 10,
				    "prHourlyLimit": 5,
				}
				""";
		FilesSilent.writeString(renovateJson5, content);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content().isEqualTo(content);
	}

	@Test
	public void testDoesNotAddSecurityLabelWhenAlreadyPresent()
			throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		String content = """
				{
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "labels": [
				        "dependencies",
				    ],
				    "minimumReleaseAge": "7 days",
				    "prConcurrentLimit": 10,
				    "prHourlyLimit": 5,
				    "vulnerabilityAlerts": {
				        "addLabels": [
				            "security",
				        ],
				        "minimumReleaseAge": "0 days",
				    },
				}
				""";
		FilesSilent.writeString(renovateJson5, content);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content().isEqualTo(content);
	}

	@Test
	public void testPreservesCommentsWhenNothingToMigrate() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		String content = """
				{
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "customManagers": [
				        {
				            // Version pins that no built-in manager sees, annotated
				            // with a "# renovate:" comment on the line above.
				            "customType": "regex",
				        },
				    ],
				    "labels": [
				        "dependencies",
				    ],
				    "minimumReleaseAge": "7 days",
				    "prConcurrentLimit": 10,
				    "prHourlyLimit": 5,
				}
				""";
		FilesSilent.writeString(renovateJson5, content);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content().isEqualTo(content);
	}

	@Test
	public void testKeepsExistingKeyOrderWhenNothingToMigrate()
			throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		String content = """
				{
				    "minimumReleaseAge": "7 days",
				    "labels": [
				        "dependencies",
				    ],
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "prConcurrentLimit": 10,
				    "prHourlyLimit": 5,
				}
				""";
		FilesSilent.writeString(renovateJson5, content);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content().isEqualTo(content);
	}

	@Test
	public void testKeepsCommentsWhenAMigrationRewritesTheFile()
			throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(renovateJson5, """
				{
				    // worth keeping
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "labels": [
				        "dependencies",
				    ],
				    "minimumReleaseAge": "4 days",
				    "prConcurrentLimit": 10,
				    "prHourlyLimit": 5,
				}
				""");

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content().isEqualTo("""
				{
				    // worth keeping
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "labels": [
				        "dependencies",
				    ],
				    "minimumReleaseAge": "7 days",
				    "prConcurrentLimit": 10,
				    "prHourlyLimit": 5,
				}
				""");
	}

	@Test
	public void testKeepsCommentsOnSeveralMembersWhenAnUnrelatedKeyChanges()
			throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(renovateJson5, """
				// Renovate keeps this repository's dependencies current.
				{
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "customManagers": [
				        {
				            // Version pins that no built-in manager sees.
				            "customType": "regex",
				        },
				    ],
				    "labels": [
				        "dependencies",
				    ],
				    "minimumReleaseAge": "4 days", // renovate's own default
				}
				""");

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content().isEqualTo("""
				// Renovate keeps this repository's dependencies current.
				{
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "customManagers": [
				        {
				            // Version pins that no built-in manager sees.
				            "customType": "regex",
				        },
				    ],
				    "labels": [
				        "dependencies",
				    ],
				    "minimumReleaseAge": "7 days", // renovate's own default
				    "prConcurrentLimit": 10,
				    "prHourlyLimit": 5,
				}
				""");
	}

	private static final String INNOVATION_DATASOURCE = """
			    "customDatasources": {
			        "graalvm-community-innovation": {
			            "defaultRegistryUrlTemplate": "https://mise-java.jdx.dev/jvm/ga/linux/x86_64.json",
			            "transformTemplates": [
			                "{\\"releases\\": [$[vendor = \\"graalvm-community\\" and \\"innovation\\" in features].{\\"version\\": version, \\"releaseTimestamp\\": created_at}]}",
			            ],
			        },
			    },
			""";

	private static final String INNOVATION_MANAGER = """
			    "customManagers": [
			        {
			            // Renovate's asdf manager maps only temurin- and
			            // adoptopenjdk- java versions to a datasource, so
			            // nothing bumps a graalvm-community pin. Innovation
			            // releases are not tagged jdk-*, so the pin is read
			            // from mise's JDK index instead.
			            "customType": "regex",
			            "datasourceTemplate": "custom.graalvm-community-innovation",
			            "depNameTemplate": "graalvm-community-innovation",
			            "managerFilePatterns": [
			                "/^\\\\.tool-versions$/",
			            ],
			            "matchStrings": [
			                "java graalvm-community-innovation-(?<currentValue>\\\\S+)",
			            ],
			            "versioningTemplate": "loose",
			        },
			    ],
			""";

	private static final String COMMON_TAIL = """
			    "extends": [
			        "config:recommended",
			    ],
			    "labels": [
			        "dependencies",
			    ],
			    "minimumReleaseAge": "7 days",
			    "prConcurrentLimit": 10,
			    "prHourlyLimit": 5,
			    "schedule": [
			        "* * 20-23 * *",
			    ],
			    "vulnerabilityAlerts": {
			        "addLabels": [
			            "security",
			        ],
			        "minimumReleaseAge": "0 days",
			        "schedule": [
			            "at any time",
			        ],
			    },
			}
			""";

	private static final String HEAD = """
			{
			    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
			    "addLabels": [
			        "{{manager}}",
			    ],
			""";

	private ChoreContext graalGithubContext() {
		FilesSilent.writeString(
				extension.root().resolve(".tool-versions"),
				"java graalvm-community-innovation-25.0.4.1.1\n"
		);
		return githubContext();
	}

	private ChoreContext temurinGithubContext() {
		FilesSilent.writeString(
				extension.root().resolve(".tool-versions"),
				"java temurin-25.0.5+8.0.LTS\n"
		);
		return githubContext();
	}

	@Test
	public void testCreatesInnovationManagerForGraalProject() throws Exception {
		new RenovateChore().doit(graalGithubContext());

		assertThat(extension.root().resolve("renovate.json5")).content()
				.isEqualTo(
						HEAD + INNOVATION_DATASOURCE + INNOVATION_MANAGER
								+ COMMON_TAIL
				);
	}

	@Test
	public void testRemovesOldGraalManager() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(
				renovateJson5,
				"""
						{
						    "customManagers": [
						        {
						            "customType": "regex",
						            "depNameTemplate": "graalvm/graalvm-ce-builds",
						            "matchStrings": [
						                "java graalvm-community-(?<currentValue>\\\\S+)",
						            ],
						        },
						    ],
						}
						"""
		);

		new RenovateChore().doit(graalGithubContext());

		assertThat(renovateJson5).content()
				.doesNotContain("graalvm-ce-builds")
				.contains("custom.graalvm-community-innovation");
	}

	@Test
	public void testKeepsExistingInnovationConfig() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		String content = HEAD + INNOVATION_DATASOURCE + INNOVATION_MANAGER
				+ COMMON_TAIL;
		content = content.replace("    \"prConcurrentLimit\": 10,\n", """
				    "packageRules": [
				        {
				            "groupName": "graalvm",
				            "matchPackageNames": [
				                "graalvm-community-innovation",
				            ],
				        },
				    ],
				    "prConcurrentLimit": 10,
				""");
		FilesSilent.writeString(renovateJson5, content);

		new RenovateChore().doit(graalGithubContext());

		assertThat(renovateJson5).content().isEqualTo(content);
	}

	@Test
	public void testNoInnovationManagerForTemurinProject() throws Exception {
		new RenovateChore().doit(temurinGithubContext());

		assertThat(extension.root().resolve("renovate.json5")).content()
				.doesNotContain("customDatasources")
				.doesNotContain("customManagers");
	}

	@Test
	public void testAddsGithubActionsVersionsForMisePin() throws Exception {
		FilesSilent.writeString(
				extension.root().resolve(".github/workflows/ci.yaml"),
				"""
						jobs:
						  build:
						    steps:
						    - env:
						        # renovate: datasource=github-releases depName=jdx/mise
						        MISE_VERSION: v2026.11.1
						      run: echo
						"""
		);

		new RenovateChore().doit(githubContext());

		assertThat(extension.root().resolve("renovate.json5")).content()
				.contains("customManagers:githubActionsVersions");
	}

	@Test
	public void testAddsJavaVersionRuleForTemurinProject() throws Exception {
		new RenovateChore().doit(temurinGithubContext());

		assertThat(extension.root().resolve("renovate.json5")).content()
				.contains("renovatebot/renovate/discussions/47778")
				.contains("""
						            "matchDatasources": [
						                "java-version",
						            ],
						            "minimumReleaseAge": null,
						""");
	}

	@Test
	public void testNoJavaVersionRuleForGraalProject() throws Exception {
		new RenovateChore().doit(graalGithubContext());

		assertThat(extension.root().resolve("renovate.json5")).content()
				.doesNotContain("java-version");
	}

	@Test
	public void testKeepsExistingPackageRules() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(renovateJson5, """
				{
				    "packageRules": [
				        {
				            "groupName": "mine",
				            "matchPackageNames": [
				                "foo",
				            ],
				        },
				    ],
				}
				""");

		new RenovateChore().doit(temurinGithubContext());
		String once = FilesSilent.readString(renovateJson5);
		new RenovateChore().doit(temurinGithubContext());

		assertThat(once).contains("\"groupName\": \"mine\"")
				.contains("java-version");
		assertThat(renovateJson5).content().isEqualTo(once);
		assertThat(once.split("\"java-version\"", -1)).hasSize(2);
	}

	private void writeWorkflowPinningATool() {
		FilesSilent.writeString(
				extension.root()
						.resolve(".github/workflows/check-actions.yaml"),
				"""
						jobs:
						  zizmor:
						    steps:
						    - env:
						        # renovate: datasource=pypi depName=zizmor
						        ZIZMOR_VERSION: 1.30.1
						      run: uvx "zizmor@${ZIZMOR_VERSION}" .
						"""
		);
	}

	@Test
	public void testAddsGitHubActionsVersionsPresetWhenAWorkflowPinsATool()
			throws Exception {
		writeWorkflowPinningATool();

		new RenovateChore().doit(githubContext());

		assertThat(extension.root().resolve("renovate.json5")).content()
				.contains("""
						    "extends": [
						        "config:recommended",
						        "customManagers:githubActionsVersions",
						    ],
						""");
	}

	@Test
	public void testKeepsGitHubActionsVersionsPresetItAlreadyHas()
			throws Exception {
		writeWorkflowPinningATool();
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		String content = """
				{
				    "extends": [
				        "customManagers:githubActionsVersions",
				    ],
				    "labels": [
				        "dependencies",
				    ],
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "minimumReleaseAge": "7 days",
				    "prConcurrentLimit": 10,
				    "prHourlyLimit": 5,
				}
				""";
		FilesSilent.writeString(renovateJson5, content);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson5).content().isEqualTo(content);
	}

	@Test
	public void testAddsNoGitHubActionsVersionsPresetWithoutPinnedTools()
			throws Exception {
		new RenovateChore().doit(githubContext());

		assertThat(extension.root().resolve("renovate.json5")).content()
				.doesNotContain("githubActionsVersions");
	}

	@Test
	public void testRemovesTheJavaVersionInputManager() throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		FilesSilent.writeString(
				renovateJson5,
				"""
						{
						    "customManagers": [
						        {
						            // Keeps the Temurin versions current.
						            "customType": "regex",
						            "matchStrings": [
						                "# renovate: datasource=(?<datasource>\\\\S+) depName=(?<depName>\\\\S+)\\\\s+java-version: (?<currentValue>\\\\S+)",
						            ],
						        },
						        {
						            // worth keeping
						            "customType": "regex",
						            "matchStrings": [
						                "java something-else-(?<currentValue>\\\\S+)",
						            ],
						        },
						    ],
						    "labels": [
						        "dependencies",
						    ],
						    "addLabels": [
						        "{{manager}}",
						    ],
						    "prConcurrentLimit": 10,
						    "prHourlyLimit": 5,
						}
						"""
		);

		new RenovateChore().doit(graalGithubContext());

		assertThat(renovateJson5).content()
				.contains("// worth keeping")
				.contains("java something-else-")
				.doesNotContain("java-version: ");
	}

	@Test
	public void testAddsNoCustomManagersToATemurinProject() throws Exception {
		FilesSilent.writeString(
				extension.root().resolve(".tool-versions"),
				"java temurin-25\n"
		);

		new RenovateChore().doit(githubContext());

		assertThat(extension.root().resolve("renovate.json5")).content()
				.doesNotContain("customManagers");
	}

}
