package io.github.arlol.chorito.tools;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class JsonBuilder {

	private final ObjectNode node;

	private JsonBuilder(ObjectNode node) {
		this.node = node;
	}

	public static JsonBuilder object() {
		return new JsonBuilder(Jsons.objectMapper().createObjectNode());
	}

	public static JsonBuilder wrap(ObjectNode node) {
		return new JsonBuilder(node);
	}

	public static JsonBuilder wrap(String content) {
		return new JsonBuilder(
				Jsons.parse(content)
						.filter(ObjectNode.class::isInstance)
						.map(n -> (ObjectNode) n)
						.orElseThrow()
		);
	}

	public JsonBuilder put(String key, String value) {
		node.put(key, value);
		return this;
	}

	public JsonBuilder array(String key, String... values) {
		ArrayNode arr = node.putArray(key);
		for (var v : values) {
			arr.add(v);
		}
		return this;
	}

	public JsonBuilder array(String key, List<String> values) {
		ArrayNode arr = node.putArray(key);
		for (var v : values) {
			arr.add(v);
		}
		return this;
	}

	public JsonBuilder object(String key, Consumer<JsonBuilder> body) {
		body.accept(new JsonBuilder(node.putObject(key)));
		return this;
	}

	public JsonBuilder migrateString(String key, String from, String to) {
		JsonNode existing = node.get(key);
		if (existing != null && from.equals(existing.asText())) {
			node.put(key, to);
		}
		return this;
	}

	public JsonBuilder ifAbsent(String key, Consumer<JsonBuilder> body) {
		if (!node.has(key)) {
			body.accept(this);
		}
		return this;
	}

	public JsonBuilder ifObjectPresent(String key, Consumer<JsonBuilder> body) {
		JsonNode child = node.get(key);
		if (child instanceof ObjectNode childObj) {
			body.accept(new JsonBuilder(childObj));
		}
		return this;
	}

	public JsonBuilder arrayAdd(String key, String... values) {
		JsonNode child = node.get(key);
		ArrayNode arr = child instanceof ArrayNode a ? a : node.putArray(key);
		for (var v : values) {
			arr.add(v);
		}
		return this;
	}

	public JsonBuilder arrayDistinctSort(String key) {
		return array(
				key,
				arrayStrings(key).stream().distinct().sorted().toList()
		);
	}

	public List<String> arrayStrings(String key) {
		JsonNode child = node.get(key);
		if (!(child instanceof ArrayNode arr)) {
			return List.of();
		}
		return StreamSupport.stream(arr.spliterator(), false)
				.filter(JsonNode::isTextual)
				.map(JsonNode::asText)
				.toList();
	}

	public String asString() {
		return Jsons.asString(Jsons.sortFields(node));
	}

}
