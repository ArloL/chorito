package io.github.arlol.chorito.tools;

import java.util.function.Consumer;

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

	public ObjectNode toNode() {
		return node;
	}

	public String asString() {
		return Jsons.asString(Jsons.sortFields(node));
	}

}
