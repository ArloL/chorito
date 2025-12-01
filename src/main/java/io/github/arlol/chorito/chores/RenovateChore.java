package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class RenovateChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path renovateJson = context.resolve("renovate.json");
		if (FilesSilent.exists(renovateJson)) {
			var currentLines = FilesSilent.readAllLines(renovateJson);
			var updatedLines = currentLines.stream().map(s -> {
				if (s.startsWith("  \"minimumReleaseAge\": \"4 days\",")) {
					return "  \"minimumReleaseAge\": \"7 days\",";
				}
				return s;
			}).toList();
			if (!currentLines.equals(updatedLines)) {
				FilesSilent.write(renovateJson, updatedLines, "\n");
			}
		} else {
			FilesSilent.writeString(
					renovateJson,
					"""
							{
							  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
							  "extends": [
							    "config:recommended"
							  ],
							  "minimumReleaseAge": "7 days",
							  "schedule": ["on the 20th day of the month"],
							  "vulnerabilityAlerts": {
							    "schedule": ["at any time"],
							    "minimumReleaseAge": "0 days"
							  }
							}
							"""
			);
			context.setDirty();
		}
		return context;
	}

}
