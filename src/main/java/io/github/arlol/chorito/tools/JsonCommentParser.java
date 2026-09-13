package io.github.arlol.chorito.tools;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Reads json5, building the tree and collecting the comments Jackson drops on
 * the way in.
 *
 * Jackson's {@code ALLOW_JAVA_COMMENTS} only makes the tokenizer tolerate a
 * comment, so the tree alone cannot be read back into the source it came from.
 * Parsing therefore happens twice over the same text: once scanning for comment
 * spans, once building the tree while recording where every field name and
 * brace sits. Each comment is then placed by where it sits relative to those
 * offsets.
 *
 * The comments go into a collector the caller owns rather than coming back
 * alongside the tree, because they go on being written to: the caller hands the
 * same collector to {@link JsonBuilder}, which keeps adding to it.
 */
public abstract class JsonCommentParser {

	private JsonCommentParser() {
	}

	private record Comment(
			int start,
			int end,
			String text
	) {
	}

	private record Member(
			ObjectNode container,
			String field,
			int nameStart
	) {
	}

	private record Container(
			JsonNode node,
			int start,
			int end
	) {
	}

	private static final class Positions {

		private final List<Member> members = new ArrayList<>();

		private final List<Container> containers = new ArrayList<>();

		private int rootStart;

	}

	/**
	 * Builds the tree of the given json5 and records every comment it finds
	 * against the member it belongs to.
	 */
	public static JsonNode parse(String content, JsonComments comments) {
		Positions positions = new Positions();
		JsonNode root = readTree(content, positions);
		for (Comment comment : scanComments(content)) {
			place(content, comment, positions, comments);
		}
		return root;
	}

	/**
	 * Collects every comment with the offsets it occupies. String literals are
	 * tracked so that a {@code //} inside a url and a {@code /*} inside a regex
	 * are left alone.
	 */
	private static List<Comment> scanComments(String content) {
		List<Comment> comments = new ArrayList<>();
		int index = 0;
		boolean inString = false;
		while (index < content.length()) {
			char current = content.charAt(index);
			if (inString) {
				if (current == '\\') {
					index += 2;
				} else {
					inString = current != '"';
					index++;
				}
			} else if (current == '"') {
				inString = true;
				index++;
			} else if (content.startsWith("//", index)) {
				int end = content.indexOf('\n', index);
				end = end < 0 ? content.length() : end;
				comments.add(
						new Comment(
								index,
								end,
								content.substring(index, end).stripTrailing()
						)
				);
				index = end;
			} else if (content.startsWith("/*", index)) {
				int end = content.indexOf("*/", index + 2);
				end = end < 0 ? content.length() : end + 2;
				comments.add(
						new Comment(index, end, deindent(content, index, end))
				);
				index = end;
			} else {
				index++;
			}
		}
		return comments;
	}

	/**
	 * Strips as much leading whitespace from the continuation lines of a block
	 * comment as there is in front of the line it starts on, so that the writer
	 * can re-indent the whole comment to wherever the member lands.
	 */
	private static String deindent(String content, int start, int end) {
		int column = start - (content.lastIndexOf('\n', start - 1) + 1);
		StringBuilder result = new StringBuilder();
		boolean first = true;
		for (String line : content.substring(start, end).lines().toList()) {
			if (first) {
				result.append(line.stripTrailing());
				first = false;
				continue;
			}
			int strip = 0;
			while (strip < column && strip < line.length()
					&& (line.charAt(strip) == ' '
							|| line.charAt(strip) == '\t')) {
				strip++;
			}
			result.append('\n').append(line.substring(strip).stripTrailing());
		}
		return result.toString();
	}

	/**
	 * Builds the tree by hand instead of through {@code readTree} so that the
	 * source offset of every field name and every brace can be recorded as it
	 * goes by.
	 */
	private static JsonNode readTree(String content, Positions positions) {
		try (JsonParser parser = Jsons.jsonFactory()
				.createParser(ObjectReadContext.empty(), content)) {
			if (parser.nextToken() == null) {
				throw new IllegalArgumentException("No json value to read");
			}
			positions.rootStart = start(parser);
			return readValue(parser, positions);
		}
	}

	private static JsonNode readValue(JsonParser parser, Positions positions) {
		return switch (parser.currentToken()) {
		case START_OBJECT -> readObject(parser, positions);
		case START_ARRAY -> readArray(parser, positions);
		default -> readScalar(parser);
		};
	}

	private static ObjectNode readObject(
			JsonParser parser,
			Positions positions
	) {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		int start = start(parser);
		while (parser.nextToken() != JsonToken.END_OBJECT) {
			String field = parser.currentName();
			int nameStart = start(parser);
			parser.nextToken();
			node.set(field, readValue(parser, positions));
			positions.members.add(new Member(node, field, nameStart));
		}
		positions.containers.add(new Container(node, start, start(parser)));
		return node;
	}

	private static ArrayNode readArray(JsonParser parser, Positions positions) {
		ArrayNode node = JsonNodeFactory.instance.arrayNode();
		int start = start(parser);
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			node.add(readValue(parser, positions));
		}
		positions.containers.add(new Container(node, start, start(parser)));
		return node;
	}

	private static JsonNode readScalar(JsonParser parser) {
		JsonNodeFactory factory = JsonNodeFactory.instance;
		return switch (parser.currentToken()) {
		case VALUE_STRING -> factory.stringNode(parser.getString());
		case VALUE_NUMBER_INT -> switch (parser.getNumberType()) {
		case INT -> factory.numberNode(parser.getIntValue());
		case LONG -> factory.numberNode(parser.getLongValue());
		default -> factory.numberNode(parser.getBigIntegerValue());
		};
		case VALUE_NUMBER_FLOAT -> factory.numberNode(parser.getDoubleValue());
		case VALUE_TRUE -> factory.booleanNode(true);
		case VALUE_FALSE -> factory.booleanNode(false);
		case VALUE_NULL -> factory.nullNode();
		default -> throw new IllegalArgumentException(
				"Unexpected json token " + parser.currentToken()
		);
		};
	}

	private static int start(JsonParser parser) {
		return (int) parser.currentTokenLocation().getCharOffset();
	}

	/**
	 * Files a comment against the member it reads as belonging to. A comment on
	 * its own line leads the next member of the object it sits in; one sharing
	 * a line with what precedes it trails the previous member. A comment with
	 * no member on the side it points at - between array elements, or after the
	 * last member of an object - has nothing to hang on and is dropped.
	 */
	private static void place(
			String content,
			Comment comment,
			Positions positions,
			JsonComments comments
	) {
		if (comment.end() <= positions.rootStart) {
			comments.addHeader(comment.text());
			return;
		}
		Container container = innermost(positions, comment);
		if (container == null
				|| !(container.node() instanceof ObjectNode objectNode)) {
			return;
		}
		if (startsItsOwnLine(content, comment.start())) {
			leadingTarget(positions, objectNode, comment).ifPresent(
					member -> comments.addLeading(
							objectNode,
							member.field(),
							comment.text()
					)
			);
		} else {
			trailingTarget(positions, objectNode, comment).ifPresent(
					member -> comments.setTrailing(
							objectNode,
							member.field(),
							comment.text()
					)
			);
		}
	}

	private static @Nullable Container innermost(
			Positions positions,
			Comment comment
	) {
		Container innermost = null;
		for (Container container : positions.containers) {
			if (container.start() < comment.start()
					&& comment.start() < container.end() && (innermost == null
							|| container.start() > innermost.start())) {
				innermost = container;
			}
		}
		return innermost;
	}

	private static Optional<Member> leadingTarget(
			Positions positions,
			ObjectNode container,
			Comment comment
	) {
		return positions.members.stream()
				.filter(member -> member.container() == container)
				.filter(member -> member.nameStart() > comment.end())
				.min(Comparator.comparingInt(Member::nameStart));
	}

	private static Optional<Member> trailingTarget(
			Positions positions,
			ObjectNode container,
			Comment comment
	) {
		return positions.members.stream()
				.filter(member -> member.container() == container)
				.filter(member -> member.nameStart() < comment.start())
				.max(Comparator.comparingInt(Member::nameStart));
	}

	private static boolean startsItsOwnLine(String content, int start) {
		for (int index = start - 1; index >= 0; index--) {
			char current = content.charAt(index);
			if (current == '\n') {
				return true;
			}
			if (current != ' ' && current != '\t') {
				return false;
			}
		}
		return true;
	}

}
