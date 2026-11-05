package io.github.arlol.chorito.chores;

import static io.github.arlol.chorito.tools.JsonMigrations.ifAbsent;
import static io.github.arlol.chorito.tools.JsonMigrations.replaceArray;
import static io.github.arlol.chorito.tools.JsonMigrations.replaceString;
import static io.github.arlol.chorito.tools.JsonMigrations.whenObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.GitHubActionsWorkflowFile;
import io.github.arlol.chorito.tools.JavaVersions;
import io.github.arlol.chorito.tools.JsonBuilder;
import io.github.arlol.chorito.tools.JsonMigration;

public class RenovateChore implements Chore {

	private static final String MINIMUM_RELEASE_AGE = "minimumReleaseAge";
	private static final String SCHEDULE = "schedule";
	// One day did not fit a busy repository's monthly updates.
	private static final String MONTHLY_WINDOW = "* * 20-23 * *";
	private static final String PR_HOURLY_LIMIT = "prHourlyLimit";
	private static final String PR_CONCURRENT_LIMIT = "prConcurrentLimit";
	private static final String LABELS = "labels";
	private static final String ADD_LABELS = "addLabels";
	private static final String CUSTOM_MANAGERS = "customManagers";
	private static final String MATCH_STRINGS = "matchStrings";
	private static final String CUSTOM_TYPE = "customType";
	private static final String REGEX = "regex";
	private static final String MANAGER_FILE_PATTERNS = "managerFilePatterns";
	private static final String EXTRACT_VERSION_TEMPLATE = "extractVersionTemplate";
	private static final String EXTENDS = "extends";
	private static final String GITHUB_ACTIONS_VERSIONS = "customManagers:githubActionsVersions";

	private static final String OLD_GRAALVM_MATCH_STRING = "java graalvm-community-(?<currentValue>\\S+)";
	private static final String INNOVATION = "graalvm-community-innovation";
	private static final String INNOVATION_MATCH_STRING = "java graalvm-community-innovation-(?<currentValue>\\S+)";
	private static final String CUSTOM_DATASOURCES = "customDatasources";
	private static final String PACKAGE_RULES = "packageRules";
	private static final String MATCH_DATASOURCES = "matchDatasources";
	private static final String JAVA_VERSION = "java-version";
	private static final String JAVA_VERSION_MATCH_STRING = "# renovate: datasource=(?<datasource>\\S+) depName=(?<depName>\\S+)\\s+java-version: (?<currentValue>\\S+)";

	private static final String INNOVATION_TRANSFORM = "{\"releases\": [$[vendor = \"graalvm-community\" and \"innovation\" in features].{\"version\": version, \"releaseTimestamp\": created_at}]}";

	private static final String GRAALVM_COMMENT = """
			Renovate's asdf manager maps only temurin- and
			adoptopenjdk- java versions to a datasource, so
			nothing bumps a graalvm-community pin. Innovation
			releases are not tagged jdk-*, so the pin is read
			from mise's JDK index instead.""";

	private static final String JAVA_VERSION_COMMENT = """
			Adoptium's release_versions has no dates, so the
			minimumReleaseAge never passes and the update stays
			pending. null turns the age check off for it.
			https://github.com/renovatebot/renovate/discussions/47778""";

	static final List<JsonMigration> MIGRATIONS = List.of(
			replaceString(MINIMUM_RELEASE_AGE, "4 days", "7 days"),
			replaceArray(
					SCHEDULE,
					List.of("on the 20th day of the month"),
					List.of(MONTHLY_WINDOW)
			),
			// Dependabot's open-pull-requests-limit, with an hourly cap so a
			// long build like drifty's is not handed ten at once.
			ifAbsent(PR_HOURLY_LIMIT, root -> root.put(PR_HOURLY_LIMIT, 5)),
			ifAbsent(
					PR_CONCURRENT_LIMIT,
					root -> root.put(PR_CONCURRENT_LIMIT, 10)
			),
			ifAbsent(
					LABELS,
					root -> root.array(LABELS, "dependencies")
							.array(ADD_LABELS, "{{manager}}")
			),
			whenObject(
					"vulnerabilityAlerts",
					ifAbsent(ADD_LABELS, a -> a.array(ADD_LABELS, "security"))
			)
	);

	/**
	 * Each manager writes its own justification into renovate.json5 as a
	 * comment, so the reason a custom manager exists is in front of whoever
	 * reads the config rather than only here.
	 */
	static final List<JsonMigration> GRAAL_MIGRATIONS = List.of(
			builder -> builder.arrayRemoveObjectsContaining(
					CUSTOM_MANAGERS,
					MATCH_STRINGS,
					OLD_GRAALVM_MATCH_STRING
			),
			// The Temurin pin used to be a java-version input with a manager of
			// its own. It is a .tool-versions java line now, which
			// Renovate's asdf manager updates.
			builder -> builder.arrayRemoveObjectsContaining(
					CUSTOM_MANAGERS,
					MATCH_STRINGS,
					JAVA_VERSION_MATCH_STRING
			),
			ifAbsent(
					CUSTOM_DATASOURCES,
					root -> root.object(
							CUSTOM_DATASOURCES,
							d -> d.object(
									INNOVATION,
									RenovateChore::innovationDatasource
							)
					)
			),
			whenObject(
					CUSTOM_DATASOURCES,
					ifAbsent(
							INNOVATION,
							d -> d.object(
									INNOVATION,
									RenovateChore::innovationDatasource
							)
					)
			),
			customManager(
					INNOVATION_MATCH_STRING,
					manager -> manager.comment(CUSTOM_TYPE, GRAALVM_COMMENT)
							.put(CUSTOM_TYPE, REGEX)
							.put("datasourceTemplate", "custom." + INNOVATION)
							.put("depNameTemplate", INNOVATION)
							.array(
									MANAGER_FILE_PATTERNS,
									"/^\\.tool-versions$/"
							)
							.array(MATCH_STRINGS, INNOVATION_MATCH_STRING)
							.put("versioningTemplate", "loose")
			)
	);

	static final List<JsonMigration> TEMURIN_MIGRATIONS = List.of(builder -> {
		if (builder.arrayHasObjectContaining(
				PACKAGE_RULES,
				MATCH_DATASOURCES,
				JAVA_VERSION
		)) {
			return builder;
		}
		return builder.arrayAddObject(
				PACKAGE_RULES,
				rule -> rule.comment(MATCH_DATASOURCES, JAVA_VERSION_COMMENT)
						.array(MATCH_DATASOURCES, JAVA_VERSION)
						.putNull(MINIMUM_RELEASE_AGE)
		);
	});

	private static final JsonMigration GITHUB_ACTIONS_VERSIONS_MIGRATION = builder -> {
		if (builder.arrayStrings(EXTENDS).contains(GITHUB_ACTIONS_VERSIONS)) {
			return builder;
		}
		return builder.arrayAdd(EXTENDS, GITHUB_ACTIONS_VERSIONS);
	};

	private static void innovationDatasource(JsonBuilder datasource) {
		datasource
				.put(
						"defaultRegistryUrlTemplate",
						"https://mise-java.jdx.dev/jvm/ga/linux/x86_64.json"
				)
				.array("transformTemplates", INNOVATION_TRANSFORM);
	}

	private static JsonMigration customManager(
			String matchString,
			Consumer<JsonBuilder> body
	) {
		return builder -> {
			if (builder.arrayHasObjectContaining(
					CUSTOM_MANAGERS,
					MATCH_STRINGS,
					matchString
			)) {
				return builder;
			}
			return builder.arrayAddObject(CUSTOM_MANAGERS, body);
		};
	}

	@Override
	public ChoreContext doit(ChoreContext context) {
		List<JsonMigration> migrations = new ArrayList<>(MIGRATIONS);
		if (JavaVersions.buildsOnGraalVm(context)) {
			migrations.addAll(GRAAL_MIGRATIONS);
		}
		if (JavaVersions.buildsOnTemurin(context)) {
			migrations.addAll(TEMURIN_MIGRATIONS);
		}
		if (pinsToolVersions(context)) {
			migrations.add(GITHUB_ACTIONS_VERSIONS_MIGRATION);
		}
		Path renovateJson = context.resolve("renovate.json");
		Path renovateJson5 = context.resolve("renovate.json5");
		if (FilesSilent.exists(renovateJson)) {
			FilesSilent.move(renovateJson, renovateJson5);
		}
		if (FilesSilent.exists(renovateJson5)) {
			var content = FilesSilent.readString(renovateJson5);
			// Comments survive the round trip, key order and formatting
			// do not: both sides come back sorted and reformatted, so
			// what is left between them is a migration actually changing
			// something. A file no migration touches is not written and
			// keeps the order and the layout its author chose.
			var asRead = JsonBuilder.wrap(content).asString();
			var migrated = JsonBuilder.wrap(content)
					.apply(migrations)
					.asString();
			if (!asRead.equals(migrated)) {
				FilesSilent.writeString(renovateJson5, migrated);
			}
		} else if (context.remotes()
				.stream()
				.anyMatch(r -> r.startsWith("https://github.com"))) {
			var content = JsonBuilder.object()
					.put(
							"$schema",
							"https://docs.renovatebot.com/renovate-schema.json"
					)
					.array(EXTENDS, "config:recommended")
					.array(LABELS, "dependencies")
					.array(ADD_LABELS, "{{manager}}")
					.put(MINIMUM_RELEASE_AGE, "7 days")
					.array(SCHEDULE, MONTHLY_WINDOW)
					.object(
							"vulnerabilityAlerts",
							v -> v.array(SCHEDULE, "at any time")
									.put(MINIMUM_RELEASE_AGE, "0 days")
									.array(ADD_LABELS, "security")
					)
					.apply(migrations)
					.asString();
			FilesSilent.writeString(renovateJson5, content);
		}
		return context;
	}

	private static boolean pinsToolVersions(ChoreContext context) {
		return DirectoryStreams.githubWorkflows(context)
				.map(FilesSilent::readString)
				.anyMatch(GitHubActionsWorkflowFile::pinsToolVersions);
	}

}
