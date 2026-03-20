package io.github.arlol.chorito.tools;

import static io.github.arlol.chorito.tools.JsonMigrations.ifAbsent;
import static io.github.arlol.chorito.tools.JsonMigrations.replaceString;
import static io.github.arlol.chorito.tools.JsonMigrations.whenObject;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

public class JsonMigrationsTest {

	// --- replaceString ---

	@Test
	public void replaceString_replacesMatchingValue() {
		var input = """
				{
				    "minimumReleaseAge": "4 days",
				}
				""";
		var result = JsonBuilder.wrap(input)
				.apply(List.of(replaceString("minimumReleaseAge", "4 days", "7 days")))
				.asString();
		assertThat(result).contains("\"7 days\"");
		assertThat(result).doesNotContain("\"4 days\"");
	}

	@Test
	public void replaceString_isNoOpWhenKeyAbsent() {
		var input = """
				{
				    "other": "value",
				}
				""";
		var result = JsonBuilder.wrap(input)
				.apply(List.of(replaceString("minimumReleaseAge", "4 days", "7 days")))
				.asString();
		assertThat(result).doesNotContain("minimumReleaseAge");
	}

	@Test
	public void replaceString_isNoOpWhenValueDoesNotMatch() {
		var input = """
				{
				    "minimumReleaseAge": "14 days",
				}
				""";
		var result = JsonBuilder.wrap(input)
				.apply(List.of(replaceString("minimumReleaseAge", "4 days", "7 days")))
				.asString();
		assertThat(result).contains("\"14 days\"");
	}

	@Test
	public void replaceString_isIdempotentWhenAlreadyTargetValue() {
		var input = """
				{
				    "minimumReleaseAge": "7 days",
				}
				""";
		var result = JsonBuilder.wrap(input)
				.apply(List.of(replaceString("minimumReleaseAge", "4 days", "7 days")))
				.asString();
		assertThat(result).contains("\"7 days\"");
	}

	// --- ifAbsent ---

	@Test
	public void ifAbsent_appliesBodyWhenKeyAbsent() {
		var input = """
				{
				    "other": "value",
				}
				""";
		var result = JsonBuilder.wrap(input)
				.apply(List.of(ifAbsent("labels", b -> b.array("labels", "dependencies"))))
				.asString();
		assertThat(result).contains("\"labels\"");
		assertThat(result).contains("\"dependencies\"");
	}

	@Test
	public void ifAbsent_isNoOpWhenKeyPresent() {
		var input = """
				{
				    "labels": [
				        "custom",
				    ],
				}
				""";
		var result = JsonBuilder.wrap(input)
				.apply(List.of(ifAbsent("labels", b -> b.array("labels", "dependencies"))))
				.asString();
		assertThat(result).contains("\"custom\"");
		assertThat(result).doesNotContain("\"dependencies\"");
	}

	// --- whenObject ---

	@Test
	public void whenObject_appliesInnerMigrationsWhenKeyIsObject() {
		var input = """
				{
				    "vulnerabilityAlerts": {
				        "schedule": [
				            "at any time",
				        ],
				    },
				}
				""";
		var result = JsonBuilder.wrap(input)
				.apply(List.of(whenObject(
						"vulnerabilityAlerts",
						ifAbsent("addLabels", a -> a.array("addLabels", "security"))
				)))
				.asString();
		assertThat(result).contains("\"security\"");
	}

	@Test
	public void whenObject_isNoOpWhenKeyAbsent() {
		var input = """
				{
				    "other": "value",
				}
				""";
		var result = JsonBuilder.wrap(input)
				.apply(List.of(whenObject(
						"vulnerabilityAlerts",
						ifAbsent("addLabels", a -> a.array("addLabels", "security"))
				)))
				.asString();
		assertThat(result).doesNotContain("vulnerabilityAlerts");
		assertThat(result).doesNotContain("security");
	}

	@Test
	public void whenObject_isNoOpWhenKeyIsNotAnObject() {
		var input = """
				{
				    "vulnerabilityAlerts": "enabled",
				}
				""";
		var result = JsonBuilder.wrap(input)
				.apply(List.of(whenObject(
						"vulnerabilityAlerts",
						ifAbsent("addLabels", a -> a.array("addLabels", "security"))
				)))
				.asString();
		assertThat(result).doesNotContain("security");
	}

	@Test
	public void whenObject_appliesMultipleInnerMigrations() {
		var input = """
				{
				    "vulnerabilityAlerts": {
				    },
				}
				""";
		var result = JsonBuilder.wrap(input)
				.apply(List.of(whenObject(
						"vulnerabilityAlerts",
						ifAbsent("addLabels", a -> a.array("addLabels", "security")),
						ifAbsent("schedule", s -> s.array("schedule", "at any time"))
				)))
				.asString();
		assertThat(result).contains("\"security\"");
		assertThat(result).contains("\"at any time\"");
	}

	// --- integration ---

	@Test
	public void integration_renovateScenario() {
		var input = """
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
				    "minimumReleaseAge": "0 days"
				  }
				}
				""";
		var migrations = List.of(
				replaceString("minimumReleaseAge", "4 days", "7 days"),
				ifAbsent("labels", root -> root
						.array("labels", "dependencies")
						.array("addLabels", "{{manager}}")),
				whenObject(
						"vulnerabilityAlerts",
						ifAbsent("addLabels", a -> a.array("addLabels", "security"))
				)
		);
		var result = JsonBuilder.wrap(input).apply(migrations).asString();
		assertThat(result).contains("\"7 days\"");
		assertThat(result).doesNotContain("\"4 days\"");
		assertThat(result).contains("\"security\"");
	}

}
