package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonFactoryBuilder;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.Feature;
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

	/**
	 * Writes the tree out, putting each comment back on the member it belongs
	 * to.
	 *
	 * The tree is walked here rather than handed to {@code writeValueAsString}
	 * because a comment has to be pushed into the printer before the field name
	 * it belongs to is written, and only a walk that knows the member can do
	 * that.
	 */
	public static String asString(JsonNode node, JsonComments comments) {
		StringWriter writer = new StringWriter();
		CustomPrettyPrinter printer = prettyPrinter();
		try (JsonGenerator generator = objectMapper().createGenerator(writer)) {
			generator.setPrettyPrinter(printer);
			for (String comment : comments.header()) {
				generator.writeRaw(comment);
				generator.writeRaw('\n');
			}
			write(node, comments, printer, generator);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return writer.toString() + "\n";
	}

	private static void write(
			JsonNode node,
			JsonComments comments,
			CustomPrettyPrinter printer,
			JsonGenerator generator
	) throws IOException {
		if (node instanceof ObjectNode objectNode) {
			generator.writeStartObject();
			@Nullable
			String previous = null;
			for (Map.Entry<String, JsonNode> entry : objectNode.properties()) {
				printer.pending(
						comments.leading(objectNode, entry.getKey()),
						trailing(comments, objectNode, previous)
				);
				generator.writeFieldName(entry.getKey());
				write(entry.getValue(), comments, printer, generator);
				previous = entry.getKey();
			}
			printer.pending(
					List.of(),
					trailing(comments, objectNode, previous)
			);
			generator.writeEndObject();
		} else if (node instanceof ArrayNode arrayNode) {
			generator.writeStartArray();
			for (JsonNode item : arrayNode) {
				write(item, comments, printer, generator);
			}
			generator.writeEndArray();
		} else {
			writeScalar(node, generator);
		}
	}

	private static @Nullable String trailing(
			JsonComments comments,
			ObjectNode container,
			@Nullable String field
	) {
		return field == null ? null
				: comments.trailing(container, field).orElse(null);
	}

	private static void writeScalar(JsonNode node, JsonGenerator generator)
			throws IOException {
		switch (node.getNodeType()) {
		case STRING -> generator.writeString(node.textValue());
		case NUMBER -> writeNumber(node, generator);
		case BOOLEAN -> generator.writeBoolean(node.booleanValue());
		case NULL -> generator.writeNull();
		case BINARY -> generator.writeBinary(node.binaryValue());
		default -> throw new IllegalArgumentException(
				"Unexpected json node " + node.getNodeType()
		);
		}
	}

	private static void writeNumber(JsonNode node, JsonGenerator generator)
			throws IOException {
		if (node.isInt()) {
			generator.writeNumber(node.intValue());
		} else if (node.isLong()) {
			generator.writeNumber(node.longValue());
		} else if (node.isBigInteger()) {
			generator.writeNumber(node.bigIntegerValue());
		} else if (node.isBigDecimal()) {
			generator.writeNumber(node.decimalValue());
		} else if (node.isFloat()) {
			generator.writeNumber(node.floatValue());
		} else {
			generator.writeNumber(node.doubleValue());
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
			updateNode.properties().forEach(entry -> {
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

	/**
	 * Sorts every object in the tree by field name, in place.
	 *
	 * Rebuilding the objects instead would hand back nodes that
	 * {@link JsonComments} has never seen, and every comment would be lost here
	 * rather than at the parser.
	 */
	public static JsonNode sortFields(JsonNode node) {
		if (node instanceof ObjectNode objectNode) {
			Map<String, JsonNode> sortedMap = new TreeMap<>();
			objectNode.properties()
					.forEach(e -> sortedMap.put(e.getKey(), e.getValue()));

			objectNode.removeAll();
			sortedMap.forEach(
					(field, value) -> objectNode.set(field, sortFields(value))
			);
			return objectNode;
		} else if (node instanceof ArrayNode arrayNode) {
			for (int i = 0; i < arrayNode.size(); i++) {
				arrayNode.set(i, sortFields(arrayNode.get(i)));
			}
			return arrayNode;
		}
		return node;
	}

}
