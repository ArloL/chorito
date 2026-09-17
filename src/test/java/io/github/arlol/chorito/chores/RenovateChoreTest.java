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
						    "schedule": [
						        "on the 20th day of the month",
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
								    "schedule": [
								        "on the 20th day of the month",
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
						  "schedule": ["on the 20th day of the month"],
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
								    "schedule": [
								        "on the 20th day of the month",
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
						  "minimumReleaseAge": "7 days"
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
				    "schedule": [
				        "on the 20th day of the month",
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
				}
				""");
	}

	private ChoreContext graalGithubContext() {
		FilesSilent.writeString(
				extension.root().resolve(".tool-versions"),
				"java graalvm-community-25.0.2\n"
		);
		return githubContext();
	}

	@Test
	public void testCreatesCustomManagersForGraalProject() throws Exception {
		new RenovateChore().doit(graalGithubContext());

		Path renovateJson5 = extension.root().resolve("renovate.json5");
		assertThat(renovateJson5).content()
				.isEqualTo(
						"""
								{
								    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
								    "addLabels": [
								        "{{manager}}",
								    ],
								    "customManagers": [
								        {
								            // Renovate's mise manager maps only temurin- and
								            // adoptopenjdk- java versions to a datasource, so
								            // nothing bumps a graalvm-community pin. The jdk-*
								            // tags are what mise offers as graalvm-community-*.
								            "customType": "regex",
								            "datasourceTemplate": "github-releases",
								            "depNameTemplate": "graalvm/graalvm-ce-builds",
								            "extractVersionTemplate": "^jdk-(?<version>\\\\S+)",
								            "managerFilePatterns": [
								                "/^\\\\.tool-versions$/",
								            ],
								            "matchStrings": [
								                "java graalvm-community-(?<currentValue>\\\\S+)",
								            ],
								        },
								    ],
								    "extends": [
								        "config:recommended",
								    ],
								    "labels": [
								        "dependencies",
								    ],
								    "minimumReleaseAge": "7 days",
								    "schedule": [
								        "on the 20th day of the month",
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
	public void testKeepsCustomManagersAndCommentsItAlreadyHas()
			throws Exception {
		Path renovateJson5 = extension.root().resolve("renovate.json5");
		String content = """
				{
				    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
				    "addLabels": [
				        "{{manager}}",
				    ],
				    "customManagers": [
				        {
				            // Renovate's mise manager maps only temurin- and
				            // adoptopenjdk- java versions to a datasource.
				            "customType": "regex",
				            "datasourceTemplate": "github-releases",
				            "depNameTemplate": "graalvm/graalvm-ce-builds",
				            "extractVersionTemplate": "^jdk-(?<version>\\\\S+)",
				            "managerFilePatterns": [
				                "/^\\\\.tool-versions$/",
				            ],
				            "matchStrings": [
				                "java graalvm-community-(?<currentValue>\\\\S+)",
				            ],
				        },
				    ],
				    "extends": [
				        "config:recommended",
				    ],
				    "labels": [
				        "dependencies",
				    ],
				    "minimumReleaseAge": "7 days",
				    "schedule": [
				        "on the 20th day of the month",
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

		new RenovateChore().doit(graalGithubContext());

		assertThat(renovateJson5).content().isEqualTo(content);
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
						                "java graalvm-community-(?<currentValue>\\\\S+)",
						            ],
						        },
						    ],
						    "labels": [
						        "dependencies",
						    ],
						    "addLabels": [
						        "{{manager}}",
						    ],
						}
						"""
		);

		new RenovateChore().doit(graalGithubContext());

		assertThat(renovateJson5).content()
				.isEqualTo(
						"""
								{
								    "addLabels": [
								        "{{manager}}",
								    ],
								    "customManagers": [
								        {
								            // worth keeping
								            "customType": "regex",
								            "matchStrings": [
								                "java graalvm-community-(?<currentValue>\\\\S+)",
								            ],
								        },
								    ],
								    "labels": [
								        "dependencies",
								    ],
								}
								"""
				);
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
