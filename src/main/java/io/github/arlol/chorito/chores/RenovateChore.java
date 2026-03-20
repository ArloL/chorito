package io.github.arlol.chorito.chores;

import static io.github.arlol.chorito.tools.JsonMigrations.ifAbsent;
import static io.github.arlol.chorito.tools.JsonMigrations.replaceString;
import static io.github.arlol.chorito.tools.JsonMigrations.whenObject;

import java.nio.file.Path;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsonBuilder;
import io.github.arlol.chorito.tools.JsonMigration;

public class RenovateChore implements Chore {

	static final List<JsonMigration> MIGRATIONS = List.of(
			replaceString("minimumReleaseAge", "4 days", "7 days"),
			ifAbsent(
					"labels",
					root -> root.array("labels", "dependencies")
							.array("addLabels", "{{manager}}")
			),
			whenObject(
					"vulnerabilityAlerts",
					ifAbsent("addLabels", a -> a.array("addLabels", "security"))
			)
	);

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
					.apply(MIGRATIONS)
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
