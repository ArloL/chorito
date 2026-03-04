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

}
