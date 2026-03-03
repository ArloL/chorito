package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.Jsons;

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
			Jsons.parse(content).ifPresent(node -> {
				if (!(node instanceof ObjectNode root)) {
					return;
				}

				JsonNode releaseAge = root.get("minimumReleaseAge");
				if (releaseAge != null
						&& "4 days".equals(releaseAge.asText())) {
					root.put("minimumReleaseAge", "7 days");
				}

				if (!root.has("labels")) {
					root.putArray("labels").add("dependencies");
					root.putArray("addLabels").add("{{manager}}");
				}

				JsonNode vulnAlerts = root.get("vulnerabilityAlerts");
				if (vulnAlerts instanceof ObjectNode vulnNode
						&& !vulnNode.has("addLabels")) {
					vulnNode.putArray("addLabels").add("security");
				}

				var newContent = Jsons.asString(root);
				if (!newContent.equalsIgnoreCase(content)) {
					FilesSilent.writeString(renovateJson5, newContent);
				}
			});
		} else if (context.remotes()
				.stream()
				.anyMatch(s -> s.startsWith("https://github.com"))) {
			ObjectNode root = Jsons.objectMapper().createObjectNode();
			root.put(
					"$schema",
					"https://docs.renovatebot.com/renovate-schema.json"
			);
			root.putArray("extends").add("config:recommended");
			root.putArray("labels").add("dependencies");
			root.putArray("addLabels").add("{{manager}}");
			root.put("minimumReleaseAge", "7 days");
			root.putArray("schedule").add("on the 20th day of the month");
			ObjectNode vulnAlerts = root.putObject("vulnerabilityAlerts");
			vulnAlerts.putArray("schedule").add("at any time");
			vulnAlerts.put("minimumReleaseAge", "0 days");
			vulnAlerts.putArray("addLabels").add("security");
			FilesSilent.writeString(renovateJson5, Jsons.asString(root));
			context.setDirty();
		}
		return context;
	}

}
