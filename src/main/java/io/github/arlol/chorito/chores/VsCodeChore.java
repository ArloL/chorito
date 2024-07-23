package io.github.arlol.chorito.chores;

import static java.util.Collections.emptyList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.arlol.chorito.filter.FileHasNoParentDirectoryWithFileFilter;
import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.Jsons;
import io.github.arlol.chorito.tools.MyPaths;

public class VsCodeChore implements Chore {

	private static Logger LOG = LoggerFactory.getLogger(VsCodeChore.class);

	@Override
	public ChoreContext doit(ChoreContext context) {
		AtomicBoolean changed = new AtomicBoolean();
		Stream.of(
				context.textFiles()
						.stream()
						.filter(file -> file.endsWith("pom.xml"))
						.map(MyPaths::getParent)
						.filter(
								file -> FileHasNoParentDirectoryWithFileFilter
										.filter(file, "pom.xml")
						),
				context.textFiles()
						.stream()
						.filter(file -> file.endsWith("gradlew"))
						.map(MyPaths::getParent),
				context.textFiles()
						.stream()
						.filter(file -> file.endsWith("mvnw"))
						.map(MyPaths::getParent),
				context.textFiles()
						.stream()
						.filter(
								file -> file.endsWith(".vscode/extensions.json")
						)
						.map(MyPaths::getParent)
						.map(MyPaths::getParent),
				context.textFiles()
						.stream()
						.filter(file -> file.endsWith(".vscode/settings.json"))
						.map(MyPaths::getParent)
						.map(MyPaths::getParent)
		).flatMap(Function.identity()).forEach(pomDir -> {
			Path settings = pomDir.resolve(".vscode/settings.json");
			Path extensions = pomDir.resolve(".vscode/extensions.json");

			if (FilesSilent.anyNotExists(settings, extensions)) {
				changed.set(true);
			}

			String templateSettings = ClassPathFiles
					.readString("vscode-settings/settings.json");
			FilesSilent.writeString(settings, templateSettings);

			List<String> recommendations = new ArrayList<>();
			recommendations.add("editorconfig.editorconfig");

			if (FilesSilent.anyChildExists(pomDir, "mvnw", "pom.xml")) {
				recommendations.add("vscjava.vscode-java-pack");
			}

			if (FilesSilent.anyChildExists(pomDir, "gradlew", "build.gradle")) {
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

			FilesSilent
					.writeString(extensions, Jsons.asString(jsonObject) + "\n");

		});
		if (changed.get()) {
			return context.refresh();
		}
		return context;
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
