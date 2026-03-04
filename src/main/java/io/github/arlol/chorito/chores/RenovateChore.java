package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsonBuilder;

public class RenovateChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path renovateJson = context.resolve("renovate.json");
		Path renovateJson5 = context.resolve("renovate.json5");
		if (FilesSilent.exists(renovateJson)) {
			FilesSilent.move(renovateJson, renovateJson5);
			context.setDirty();
		}
		if (FilesSilent.exists(renovateJson5)) {
			var content = FilesSilent.readString(renovateJson5);
			var newContent = JsonBuilder.wrap(content)
					.migrateString("minimumReleaseAge", "4 days", "7 days")
					.ifAbsent(
							"labels",
							root -> root.array("labels", "dependencies")
									.array("addLabels", "{{manager}}")
					)
					.ifObjectPresent(
							"vulnerabilityAlerts",
							v -> v.ifAbsent(
									"addLabels",
									a -> a.array("addLabels", "security")
							)
					)
					.asString();
			if (!newContent.equalsIgnoreCase(content)) {
				FilesSilent.writeString(renovateJson5, newContent);
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
					.array("labels", "dependencies")
					.array("addLabels", "{{manager}}")
					.put("minimumReleaseAge", "7 days")
					.array("schedule", "on the 20th day of the month")
					.object(
							"vulnerabilityAlerts",
							v -> v.array("schedule", "at any time")
									.put("minimumReleaseAge", "0 days")
									.array("addLabels", "security")
					)
					.asString();
			FilesSilent.writeString(renovateJson5, content);
			context.setDirty();
		}
		return context;
	}

}
