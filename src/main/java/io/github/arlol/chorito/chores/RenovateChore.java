package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.stream.Stream;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class RenovateChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path renovateJson = context.resolve("renovate.json");
		if (FilesSilent.exists(renovateJson)) {
			var currentLines = FilesSilent.readAllLines(renovateJson);
			boolean hasLabels = currentLines.stream()
					.anyMatch(s -> s.contains("\"labels\""));
			boolean hasSecurityLabel = currentLines.stream()
					.anyMatch(s -> s.contains("\"addLabels\": [\"security\"]"));
			var updatedLines = currentLines.stream().flatMap(s -> {
				if (s.startsWith("  \"minimumReleaseAge\": \"4 days\",")) {
					return Stream.of("  \"minimumReleaseAge\": \"7 days\",");
				}
				if (s.equals("  ],") && !hasLabels) {
					return Stream.of(
							s,
							"  \"labels\": [\"dependencies\"],",
							"  \"addLabels\": [\"{{manager}}\"],"
					);
				}
				if (s.equals("    \"minimumReleaseAge\": \"0 days\"")
						&& !hasSecurityLabel) {
					return Stream.of(
							"    \"minimumReleaseAge\": \"0 days\",",
							"    \"addLabels\": [\"security\"]"
					);
				}
				return Stream.of(s);
			}).toList();
			if (!currentLines.equals(updatedLines)) {
				FilesSilent.write(renovateJson, updatedLines, "\n");
			}
		} else if (context.remotes()
				.stream()
				.anyMatch(s -> s.startsWith("https://github.com"))) {
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
							  "schedule": ["on the 20th day of the month"],
							  "vulnerabilityAlerts": {
							    "schedule": ["at any time"],
							    "minimumReleaseAge": "0 days",
							    "addLabels": ["security"]
							  }
							}
							"""
			);
			context.setDirty();
		}
		return context;
	}

}
