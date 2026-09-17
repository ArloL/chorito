package io.github.arlol.chorito.tools;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

public final class JsonBuilder {

	private final ObjectNode node;

	private final JsonComments comments;

	private JsonBuilder(ObjectNode node, JsonComments comments) {
		this.node = node;
		this.comments = comments;
	}

	public static JsonBuilder object() {
		return new JsonBuilder(
				Jsons.objectMapper().createObjectNode(),
				new JsonComments()
		);
	}

	public static JsonBuilder wrap(ObjectNode node) {
		return new JsonBuilder(node, new JsonComments());
	}

	public static JsonBuilder wrap(String content) {
		JsonComments comments = new JsonComments();
		if (!(JsonCommentParser
				.parse(content, comments) instanceof ObjectNode objectNode)) {
			throw new IllegalArgumentException("Not a json object");
		}
		return new JsonBuilder(objectNode, comments);
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
		body.accept(new JsonBuilder(node.putObject(key), comments));
		return this;
	}

	/**
	 * Puts a comment on its own line above the given entry. The text is written
	 * without comment delimiters; each line becomes one {@code //} line in the
	 * output.
	 */
	public JsonBuilder comment(String key, String text) {
		comments.addLeading(node, key, asLineComment(text));
		return this;
	}

	private static String asLineComment(String text) {
		if (text.isEmpty()) {
			return "//";
		}
		return text.lines()
				.map(line -> line.isEmpty() ? "//" : "// " + line)
				.collect(Collectors.joining("\n"));
	}

	public JsonBuilder migrateString(String key, String from, String to) {
		JsonNode existing = node.get(key);
		if (existing != null && from.equals(existing.asString(""))) {
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
			body.accept(new JsonBuilder(childObj, comments));
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

	public JsonBuilder arrayAddObject(String key, Consumer<JsonBuilder> body) {
		JsonNode child = node.get(key);
		ArrayNode arr = child instanceof ArrayNode a ? a : node.putArray(key);
		body.accept(new JsonBuilder(arr.addObject(), comments));
		return this;
	}

	/**
	 * Whether the array at {@code key} already holds an object whose
	 * {@code childKey} array contains {@code value}.
	 */
	public boolean arrayHasObjectContaining(
			String key,
			String childKey,
			String value
	) {
		if (!(node.get(key) instanceof ArrayNode arr)) {
			return false;
		}
		return StreamSupport.stream(arr.spliterator(), false)
				.map(element -> element.get(childKey))
				.anyMatch(
						child -> child instanceof ArrayNode values
								&& StreamSupport
										.stream(values.spliterator(), false)
										.anyMatch(
												v -> v.isString() && value
														.equals(v.stringValue())
										)
				);
	}

	/**
	 * Removes every object in the array at {@code key} whose {@code childKey}
	 * array contains {@code value}, and the array itself once it is empty.
	 */
	public JsonBuilder arrayRemoveObjectsContaining(
			String key,
			String childKey,
			String value
	) {
		if (!(node.get(key) instanceof ArrayNode arr)) {
			return this;
		}
		for (int i = arr.size() - 1; i >= 0; i--) {
			if (arr.get(i).get(childKey) instanceof ArrayNode values
					&& StreamSupport.stream(values.spliterator(), false)
							.anyMatch(
									v -> v.isString()
											&& value.equals(v.stringValue())
							)) {
				arr.remove(i);
			}
		}
		if (arr.isEmpty()) {
			node.remove(key);
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
				.filter(JsonNode::isString)
				.map(JsonNode::stringValue)
				.toList();
	}

	public JsonBuilder apply(List<JsonMigration> migrations) {
		JsonBuilder current = this;
		for (JsonMigration m : migrations) {
			current = m.apply(current);
		}
		return current;
	}

	public String asString() {
		return Jsons.asString(Jsons.sortFields(node), comments);
	}

}
