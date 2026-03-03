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
	public void testCreatesRenovateJsonForGitHubRepo() throws Exception {
		new RenovateChore().doit(githubContext());

		Path renovateJson = extension.root().resolve("renovate.json");
		assertThat(renovateJson).content()
				.isEqualTo(
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
	}

	@Test
	public void testDoesNotCreateRenovateJsonForNonGitHubRepo()
			throws Exception {
		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://gitlab.com/example/example"))
				.build();

		new RenovateChore().doit(context);

		Path renovateJson = extension.root().resolve("renovate.json");
		assertThat(renovateJson).doesNotExist();
	}

	@Test
	public void testDoesNotCreateRenovateJsonWithNoRemotes() throws Exception {
		new RenovateChore().doit(extension.choreContext());

		Path renovateJson = extension.root().resolve("renovate.json");
		assertThat(renovateJson).doesNotExist();
	}

	@Test
	public void testUpdatesMinimumReleaseAgeFrom4DaysTo7Days()
			throws Exception {
		Path renovateJson = extension.root().resolve("renovate.json");
		FilesSilent.writeString(
				renovateJson,
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

		assertThat(renovateJson).content()
				.isEqualTo(
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
	}

	@Test
	public void testAddsLabelsWhenMissing() throws Exception {
		Path renovateJson = extension.root().resolve("renovate.json");
		FilesSilent.writeString(
				renovateJson,
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

		assertThat(renovateJson).content()
				.isEqualTo(
						"""
								{
								    "$schema": "https://docs.renovatebot.com/renovate-schema.json",
								    "extends": [
								        "config:recommended",
								    ],
								    "minimumReleaseAge": "7 days",
								    "labels": [
								        "dependencies",
								    ],
								    "addLabels": [
								        "{{manager}}",
								    ],
								}
								"""
				);
	}

	@Test
	public void testAddsSecurityLabelToVulnerabilityAlerts() throws Exception {
		Path renovateJson = extension.root().resolve("renovate.json");
		FilesSilent.writeString(
				renovateJson,
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

		assertThat(renovateJson).content()
				.isEqualTo(
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
	}

	@Test
	public void testDoesNotModifyAlreadyUpToDateFile() throws Exception {
		Path renovateJson = extension.root().resolve("renovate.json");
		String content = """
				{
				  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
				  "extends": [
				    "config:recommended"
				  ],
				  "labels": ["dependencies"],
				  "addLabels": ["{{manager}}"],
				  "minimumReleaseAge": "7 days",
				  "schedule": ["on the 20th day of the month"],
				  "vulnerabilityAlerts": {
				    "schedule": ["at any time"],
				    "minimumReleaseAge": "0 days",
				    "addLabels": ["security"]
				  }
				}
				""";
		FilesSilent.writeString(renovateJson, content);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson).content().isEqualTo(content);
	}

	@Test
	public void testDoesNotAddLabelsWhenAlreadyPresent() throws Exception {
		Path renovateJson = extension.root().resolve("renovate.json");
		String content = """
				{
				  "labels": ["custom"],
				  "addLabels": ["{{manager}}"],
				  "minimumReleaseAge": "7 days"
				}
				""";
		FilesSilent.writeString(renovateJson, content);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson).content().isEqualTo(content);
	}

	@Test
	public void testDoesNotAddSecurityLabelWhenAlreadyPresent()
			throws Exception {
		Path renovateJson = extension.root().resolve("renovate.json");
		String content = """
				{
				  "labels": ["dependencies"],
				  "addLabels": ["{{manager}}"],
				  "minimumReleaseAge": "7 days",
				  "vulnerabilityAlerts": {
				    "minimumReleaseAge": "0 days",
				    "addLabels": ["security"]
				  }
				}
				""";
		FilesSilent.writeString(renovateJson, content);

		new RenovateChore().doit(githubContext());

		assertThat(renovateJson).content().isEqualTo(content);
	}

}
