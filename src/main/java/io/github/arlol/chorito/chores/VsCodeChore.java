package io.github.arlol.chorito.chores;

import static java.util.Collections.emptyList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.Jsons;
import io.github.arlol.chorito.tools.MyPaths;

public class VsCodeChore implements Chore {

	private static Logger LOG = LoggerFactory.getLogger(VsCodeChore.class);

	@Override
	public ChoreContext doit(ChoreContext context) {
		Stream.of(
				DirectoryStreams.javaDirectories(context),
				context.textFiles()
						.stream()
						.map(MyPaths::getParent)
						.filter(file -> file.endsWith(".vscode"))
						.map(MyPaths::getParent)
		).flatMap(Function.identity()).distinct().forEach(dir -> {
			Path settings = dir.resolve(".vscode/settings.json");
			Path extensions = dir.resolve(".vscode/extensions.json");

			if (FilesSilent.anyNotExists(settings, extensions)) {
				context.setDirty();
			}

			if (FilesSilent.anyChildExists(
					dir,
					"mvnw",
					"pom.xml",
					"gradlew",
					"build.gradle"
			)) {
				String newSettingsJson = newSettingsJson(settings);
				FilesSilent.writeString(settings, newSettingsJson);
			}

			List<String> recommendations = new ArrayList<>();
			recommendations.add("editorconfig.editorconfig");

			if (FilesSilent.anyChildExists(dir, "mvnw", "pom.xml")) {
				recommendations.add("vscjava.vscode-java-pack");
			}

			if (FilesSilent.anyChildExists(dir, "gradlew", "build.gradle")) {
				recommendations.add("vscjava.vscode-gradle");
				recommendations.add("vscjava.vscode-java-pack");
			}

			if (FilesSilent.exists(extensions)) {
				recommendations
						.addAll(readRecommendations(Jsons.parse(extensions)));
			}

			ObjectMapper objectMapper = Jsons.objectMapper();

			ObjectNode jsonObject = objectMapper.createObjectNode();
			ArrayNode jsonArray = jsonObject.putArray("recommendations");
			recommendations.stream()
					.distinct()
					.sorted()
					.forEach(jsonArray::add);

			FilesSilent.writeString(extensions, Jsons.asString(jsonObject));

		});
		return context;
	}

	private String newSettingsJson(Path settings) {
		JsonNode template = Jsons
				.parse(
						ClassPathFiles
								.readString("vscode-settings/settings.json")
				)
				.orElseThrow(IllegalStateException::new);
		if (!FilesSilent.exists(settings)) {
			return Jsons.asString(template);
		}
		Jsons.parse(settings).ifPresent(node -> Jsons.merge(template, node));
		return Jsons.asString(Jsons.sortFields(template));
	}

	private List<String> readRecommendations(Optional<JsonNode> element) {
		try {
			return element.filter(JsonNode::isObject)
					.map(ObjectNode.class::cast)
					.map(e -> e.get("recommendations"))
					.filter(JsonNode::isArray)
					.map(ArrayNode.class::cast)
					.map(ArrayNode::spliterator)
					.map(s -> StreamSupport.stream(s, false))
					.orElseGet(() -> {
						LOG.error(
								"Could not get recommendations. Returning empty list."
						);
						return Stream.empty();
					})
					.filter(JsonNode::isTextual)
					.map(JsonNode::asText)
					.toList();
		} catch (IllegalStateException e) {
			LOG.error("Could not get recommendations", e);
			return emptyList();
		}
	}

}
