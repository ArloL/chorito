package io.github.arlol.chorito.chores;

import static io.github.arlol.chorito.tools.JsonMigrations.ifAbsent;
import static io.github.arlol.chorito.tools.JsonMigrations.replaceString;
import static io.github.arlol.chorito.tools.JsonMigrations.whenObject;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JavaVersions;
import io.github.arlol.chorito.tools.JsonBuilder;
import io.github.arlol.chorito.tools.JsonMigration;

public class RenovateChore implements Chore {

	private static final String MINIMUM_RELEASE_AGE = "minimumReleaseAge";
	private static final String LABELS = "labels";
	private static final String ADD_LABELS = "addLabels";
	private static final String CUSTOM_MANAGERS = "customManagers";
	private static final String MATCH_STRINGS = "matchStrings";
	private static final String CUSTOM_TYPE = "customType";
	private static final String REGEX = "regex";
	private static final String MANAGER_FILE_PATTERNS = "managerFilePatterns";
	private static final String EXTRACT_VERSION_TEMPLATE = "extractVersionTemplate";

	private static final String GRAALVM_MATCH_STRING = "java graalvm-community-(?<currentValue>\\S+)";
	private static final String JAVA_VERSION_MATCH_STRING = "# renovate: datasource=(?<datasource>\\S+) depName=(?<depName>\\S+)\\s+java-version: (?<currentValue>\\S+)";

	static final List<JsonMigration> MIGRATIONS = List.of(
			replaceString(MINIMUM_RELEASE_AGE, "4 days", "7 days"),
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
	 * Renovate's mise manager maps only temurin- and adoptopenjdk- java
	 * versions to a datasource and returns undefined for anything else, so
	 * nothing bumps a graalvm-community pin. The jdk-* tags are exactly what
	 * mise offers as graalvm-community-*; the graal-* and vm-* ones are
	 * GraalVM's own versioning and mise installs none of them.
	 * <p>
	 * The second manager keeps the versions the jobs that build no native image
	 * pin current. Adoptium's own semver carries a build suffix, as in
	 * 25.0.4+101.0.LTS, which setup-java cannot resolve, so only the release
	 * version is kept.
	 */
	static final List<JsonMigration> GRAAL_MIGRATIONS = List.of(
			customManager(
					GRAALVM_MATCH_STRING,
					manager -> manager.put(CUSTOM_TYPE, REGEX)
							.put("datasourceTemplate", "github-releases")
							.put("depNameTemplate", "graalvm/graalvm-ce-builds")
							.put(
									EXTRACT_VERSION_TEMPLATE,
									"^jdk-(?<version>\\S+)"
							)
							.array(
									MANAGER_FILE_PATTERNS,
									"/^\\.tool-versions$/"
							)
							.array(MATCH_STRINGS, GRAALVM_MATCH_STRING)
			),
			customManager(
					JAVA_VERSION_MATCH_STRING,
					manager -> manager.put(CUSTOM_TYPE, REGEX)
							.put(
									EXTRACT_VERSION_TEMPLATE,
									"^(?<version>\\d+\\.\\d+\\.\\d+)"
							)
							.array(
									MANAGER_FILE_PATTERNS,
									"/^\\.github/workflows/[^/]+\\.ya?ml$/"
							)
							.array(MATCH_STRINGS, JAVA_VERSION_MATCH_STRING)
			)
	);

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
		List<JsonMigration> migrations = MIGRATIONS;
		if (JavaVersions.buildsOnGraalVm(context)) {
			migrations = Stream
					.concat(MIGRATIONS.stream(), GRAAL_MIGRATIONS.stream())
					.toList();
		}
		Path renovateJson = context.resolve("renovate.json");
		Path renovateJson5 = context.resolve("renovate.json5");
		if (FilesSilent.exists(renovateJson)) {
			FilesSilent.move(renovateJson, renovateJson5);
			context.setDirty();
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
					.array("extends", "config:recommended")
					.array(LABELS, "dependencies")
					.array(ADD_LABELS, "{{manager}}")
					.put(MINIMUM_RELEASE_AGE, "7 days")
					.array("schedule", "on the 20th day of the month")
					.object(
							"vulnerabilityAlerts",
							v -> v.array("schedule", "at any time")
									.put(MINIMUM_RELEASE_AGE, "0 days")
									.array(ADD_LABELS, "security")
					)
					.apply(migrations)
					.asString();
			FilesSilent.writeString(renovateJson5, content);
			context.setDirty();
		}
		return context;
	}

}
