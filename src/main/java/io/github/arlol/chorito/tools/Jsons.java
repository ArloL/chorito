package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.core.util.Separators.Spacing;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class Jsons {

	private Jsons() {
	}

	public static ObjectMapper objectMapper() {
		var jsonFactory = new JsonFactoryBuilder()
				.enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
				.build();
		ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.enable(Feature.ALLOW_COMMENTS);
		objectMapper.setDefaultPrettyPrinter(prettyPrinter());
		return objectMapper;
	}

	public static CustomPrettyPrinter prettyPrinter() {
		return new CustomPrettyPrinter(
				new Separators().withObjectFieldValueSpacing(Spacing.AFTER)
		);
	}

	public static String asString(Object object) {
		try {
			return objectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(object) + "\n";
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Optional<JsonNode> parse(Path extensions) {
		try {
			return Optional.ofNullable(
					objectMapper().readTree(Files.newInputStream(extensions))
			);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Optional<JsonNode> parse(String json) {
		try {
			return Optional.ofNullable(objectMapper().readTree(json));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
		if (mainNode instanceof ObjectNode mainObjectNode) {
			updateNode.fields().forEachRemaining(entry -> {
				String fieldName = entry.getKey();
				JsonNode jsonNode = entry.getValue();
				if (mainObjectNode.has(fieldName)) {
					JsonNode existingNode = mainObjectNode.get(fieldName);
					if (existingNode.isObject()) {
						merge(existingNode, jsonNode);
					} else {
						mainObjectNode.set(fieldName, jsonNode);
					}
				} else {
					mainObjectNode.set(fieldName, jsonNode);
				}
			});
		}
		return mainNode;
	}

	public static JsonNode sortFields(JsonNode node) {
		if (node instanceof ObjectNode objectNode) {
			Map<String, JsonNode> sortedMap = new TreeMap<>();
			objectNode.fields()
					.forEachRemaining(
							e -> sortedMap.put(e.getKey(), e.getValue())
					);

			ObjectNode sortedObjectNode = objectMapper().createObjectNode();
			sortedMap.forEach(sortedObjectNode::set);
			return sortedObjectNode;
		} else if (node instanceof ArrayNode arrayNode) {
			ArrayNode sortedArrayNode = objectMapper().createArrayNode();
			for (var item : arrayNode) {
				JsonNode sortedElement = sortFields(item);
				sortedArrayNode.add(sortedElement);
			}
			return sortedArrayNode;
		}
		return node;
	}

}
