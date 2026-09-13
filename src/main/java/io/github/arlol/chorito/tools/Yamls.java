package io.github.arlol.chorito.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.comments.CommentLine;
import org.snakeyaml.engine.v2.comments.CommentType;
import org.snakeyaml.engine.v2.common.FlowStyle;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.composer.Composer;
import org.snakeyaml.engine.v2.emitter.Emitter;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.scanner.StreamReader;
import org.snakeyaml.engine.v2.serializer.Serializer;

public abstract class Yamls {

	private static final Pattern YAML_PATH_ATTRIBUTE_SELECTOR_PATTERN = Pattern
			.compile(
					"(" + "[a-zA-Z]+" + ")" + "\\[" + "(" + "[a-zA-Z]+" + ")"
							+ "=" + "(" + "[a-zA-Z]+" + ")" + "\\]"
			);

	private Yamls() {
	}

	public static Optional<SequenceNode> getKeyAsSequence(
			MappingNode map,
			String key
	) {
		return nodeAsSequence(getKeyAsNode(map, key));
	}

	public static Optional<SequenceNode> getKeyAsSequence(
			Optional<MappingNode> map,
			String key
	) {
		return nodeAsSequence(getKeyAsNode(map, key));
	}

	public static Optional<ScalarNode> getKeyAsScalar(
			MappingNode map,
			String key
	) {
		return getKeyAsScalar(Optional.of(map), key);
	}

	public static Optional<ScalarNode> getKeyAsScalar(
			Optional<MappingNode> map,
			String key
	) {
		return nodeAsScalar(getKeyAsNode(map, key));
	}

	public static Optional<Node> getKeyAsNode(MappingNode map, String key) {
		return getKeyAsNode(Optional.of(map), key);
	}

	public static Optional<Node> getKeyAsNode(
			Optional<MappingNode> map,
			String key
	) {
		return getKeyAsTuple(map, key).map(NodeTuple::getValueNode);
	}

	public static Optional<NodeTuple> getKeyAsTuple(
			Optional<MappingNode> map,
			String key
	) {
		return map.map(MappingNode::getValue)
				.map(List::stream)
				.orElse(Stream.empty())
				.filter(t -> {
					if (t.getKeyNode() instanceof ScalarNode keyNode
							&& key.equals(keyNode.getValue())) {
						return true;
					}
					return false;
				})
				.findFirst();
	}

	public static Optional<String> scalarValue(Optional<Node> node) {
		return nodeAsScalar(node).map(ScalarNode::getValue);
	}

	public static Optional<ScalarNode> nodeAsScalar(Optional<Node> node) {
		return node.filter(n -> n instanceof ScalarNode)
				.map(n -> (ScalarNode) n);
	}

	public static List<MappingNode> nodeAsMap(List<Node> nodes) {
		return nodes.stream().map(Yamls::nodeAsMap).toList();
	}

	public static MappingNode nodeAsMap(Node node) {
		return nodeAsMap(Optional.of(node)).orElseThrow();
	}

	public static Optional<MappingNode> nodeAsMap(Optional<Node> node) {
		return node.filter(n -> n instanceof MappingNode)
				.map(n -> (MappingNode) n);
	}

	public static Optional<SequenceNode> nodeAsSequence(Node node) {
		return nodeAsSequence(Optional.of(node));
	}

	public static Optional<SequenceNode> nodeAsSequence(Optional<Node> node) {
		return node.filter(n -> n instanceof SequenceNode)
				.map(n -> (SequenceNode) n);
	}

	public static SequenceNode newSequence(Node... nodes) {
		return newSequence(new ArrayList<>(List.of(nodes)));
	}

	public static SequenceNode newSequence(List<Node> values) {
		return new SequenceNode(Tag.SEQ, values, FlowStyle.BLOCK);
	}

	public static MappingNode newMap(NodeTuple... nodes) {
		return newMap(new ArrayList<>(List.of(nodes)));
	}

	public static MappingNode newMap(List<NodeTuple> nodes) {
		return new MappingNode(Tag.MAP, nodes, FlowStyle.BLOCK);
	}

	public static ScalarNode newScalar(boolean value) {
		return new ScalarNode(
				Tag.BOOL,
				String.valueOf(value),
				ScalarStyle.PLAIN
		);
	}

	public static ScalarNode newScalar(int value) {
		return new ScalarNode(
				Tag.INT,
				String.valueOf(value),
				ScalarStyle.PLAIN
		);
	}

	public static ScalarNode newScalar(String value) {
		return newScalar(value, ScalarStyle.DOUBLE_QUOTED);
	}

	public static ScalarNode newScalar(String value, ScalarStyle style) {
		return new ScalarNode(Tag.STR, value, style);
	}

	public static NodeTuple newTuple(String key, Node value) {
		return newTuple(newScalar(key, ScalarStyle.PLAIN), value);
	}

	public static NodeTuple newTuple(Node keyNode, Node value) {
		return new NodeTuple(keyNode, value);
	}

	public static Optional<Node> load(String content) {
		LoadSettings loadSettings = LoadSettings.builder()
				.setParseComments(true)
				.build();
		Optional<Node> root = new Composer(
				loadSettings,
				new ParserImpl(
						loadSettings,
						new StreamReader(loadSettings, content)
				)
		).getSingleNode();
		root.ifPresent(node -> placeComments(node, content));
		return root;
	}

	/**
	 * Puts the comments the composer filed against the wrong node back where
	 * they were written, so that a document chorito only reads comes out as it
	 * went in.
	 */
	private static void placeComments(Node root, String content) {
		List<Node> nodes = inDocumentOrder(root, new ArrayList<>());
		leadNextNode(nodes, content.split("\n", -1));
		moveLeadingCommentsToSequenceItems(root);
	}

	private static List<Node> inDocumentOrder(Node node, List<Node> nodes) {
		nodes.add(node);
		if (node instanceof MappingNode mappingNode) {
			mappingNode.getValue().forEach(tuple -> {
				inDocumentOrder(tuple.getKeyNode(), nodes);
				inDocumentOrder(tuple.getValueNode(), nodes);
			});
		} else if (node instanceof SequenceNode sequenceNode) {
			sequenceNode.getValue()
					.forEach(item -> inDocumentOrder(item, nodes));
		}
		return nodes;
	}

	/**
	 * Hands a comment the scanner read as trailing a value, but which was
	 * written on a line of its own, to the node that follows it.
	 *
	 * A block scalar swallows the newline that ends it, so a comment on the
	 * next line arrives as an in-line comment of that scalar. The emitter
	 * writes an in-line comment where the value ended, which for a block scalar
	 * is column 0, and the comment loses its indentation along with the entry
	 * it was heading. Giving it to the next node in document order is what the
	 * composer would have done had it read the comment as a block comment, and
	 * the ordinary placement takes over from there - the next node being a
	 * later key in the same mapping, or the next sequence item, is what decides
	 * which.
	 *
	 * A comment with no node after it keeps its place, having nothing to lead.
	 */
	private static void leadNextNode(List<Node> nodes, String[] lines) {
		for (int index = 0; index < nodes.size() - 1; index++) {
			Node node = nodes.get(index);
			Node next = nodes.get(index + 1);
			List<CommentLine> inLine = node.getInLineComments();
			if (inLine == null || inLine.isEmpty()) {
				continue;
			}
			List<CommentLine> leading = new ArrayList<>();
			List<CommentLine> trailing = new ArrayList<>();
			for (CommentLine comment : inLine) {
				boolean leads = startsItsOwnLine(lines, comment)
						&& startsBefore(comment, next);
				(leads ? leading : trailing).add(comment);
			}
			if (leading.isEmpty()) {
				continue;
			}
			node.setInLineComments(trailing);
			next.setBlockComments(
					append(asBlockComments(leading), next.getBlockComments())
			);
		}
	}

	private static List<CommentLine> asBlockComments(
			List<CommentLine> comments
	) {
		return comments.stream()
				.map(
						comment -> new CommentLine(
								comment.getStartMark(),
								comment.getEndMark(),
								comment.getValue(),
								CommentType.BLOCK
						)
				)
				.toList();
	}

	/**
	 * Joins two lists of comments in the order they were written, treating the
	 * null a node without comments reports as empty.
	 */
	private static List<CommentLine> append(
			@Nullable List<CommentLine> first,
			@Nullable List<CommentLine> second
	) {
		List<CommentLine> comments = new ArrayList<>();
		if (first != null) {
			comments.addAll(first);
		}
		if (second != null) {
			comments.addAll(second);
		}
		return comments;
	}

	private static boolean startsItsOwnLine(
			String[] lines,
			CommentLine comment
	) {
		return comment.getStartMark().filter(mark -> {
			if (mark.getLine() >= lines.length) {
				return false;
			}
			String line = lines[mark.getLine()];
			return line.substring(0, Math.min(mark.getColumn(), line.length()))
					.isBlank();
		}).isPresent();
	}

	/**
	 * Whether the comment was written above the given node rather than below
	 * it. The end of a document reads as an in-line comment of the root, which
	 * begins long before it.
	 */
	private static boolean startsBefore(CommentLine comment, Node next) {
		return comment.getStartMark()
				.flatMap(
						mark -> next.getStartMark()
								.map(start -> mark.getLine() < start.getLine())
				)
				.orElse(false);
	}

	/**
	 * Moves the comment that leads a sequence item off the first key of the
	 * item's mapping, where the composer attaches it, and onto the item itself.
	 *
	 * The emitter writes the {@code -} indicator before it descends into the
	 * mapping, so a comment left on the key can only come out after the
	 * indicator - a heading above a step ends up as the step's first line.
	 *
	 * Carrying it on the item is not enough on its own. The emitter places a
	 * block comment by the column its start mark records: at or left of the
	 * indicator goes above the {@code -}, further right stays behind it. That
	 * is what makes both written forms round-trip, and it is also the catch for
	 * a comment built by hand rather than parsed - without a start mark it
	 * lands behind the {@code -} wherever it is attached.
	 */
	private static void moveLeadingCommentsToSequenceItems(Node node) {
		if (node instanceof MappingNode mappingNode) {
			mappingNode.getValue().forEach(tuple -> {
				moveLeadingCommentsToSequenceItems(tuple.getKeyNode());
				moveLeadingCommentsToSequenceItems(tuple.getValueNode());
			});
		} else if (node instanceof SequenceNode sequenceNode) {
			sequenceNode.getValue().forEach(item -> {
				moveLeadingCommentsToSequenceItems(item);
				if (item instanceof MappingNode itemMap) {
					moveLeadingCommentsOffFirstKey(itemMap);
				}
			});
		}
	}

	private static void moveLeadingCommentsOffFirstKey(MappingNode item) {
		item.getValue().stream().findFirst().ifPresent(first -> {
			Node key = first.getKeyNode();
			List<CommentLine> comments = key.getBlockComments();
			if (comments == null || comments.isEmpty()) {
				return;
			}
			item.setBlockComments(append(item.getBlockComments(), comments));
			key.setBlockComments(List.of());
		});
	}

	public static String asString(Optional<Node> root) {
		if (root.isEmpty()) {
			return "";
		}
		DumpSettings dumpSettings = DumpSettings.builder()
				.setDumpComments(true)
				.setSplitLines(false)
				.build();

		YamlStreamToStringWriter writer = new YamlStreamToStringWriter();
		Serializer serializer = new Serializer(
				dumpSettings,
				new Emitter(dumpSettings, writer)
		);
		serializer.emitStreamStart();
		serializer.serializeDocument(root.orElseThrow());
		String string = writer.toString();
		string = string.replaceAll("\n\s+\n", "\n\n");
		if (string.endsWith("\n")) {
			return string;
		}
		return string + "\n";
	}

	public static Optional<MappingNode> getKeyAsMap(
			MappingNode map,
			String key
	) {
		return getKeyAsMap(Optional.of(map), key);
	}

	public static Optional<MappingNode> getKeyAsMap(
			Optional<MappingNode> map,
			String key
	) {
		return nodeAsMap(getKeyAsNode(map, key));
	}

	public static Optional<String> scalarValue(Node node) {
		return scalarValue(Optional.of(node));
	}

	public static Consumer<MappingNode> copyValue(
			Optional<MappingNode> template
	) {
		return node -> template
				.ifPresent(value -> node.setValue(value.getValue()));
	}

	@SuppressWarnings("null") // null analysis is unnecessarily complicated
	public static void setKey(MappingNode map, String key, Node node) {
		boolean hasKey = map.getValue()
				.stream()
				.anyMatch(
						t -> t.getKeyNode() instanceof ScalarNode keyNode
								&& key.equals(keyNode.getValue())
				);
		if (hasKey) {
			var newValue = map.getValue().stream().map(t -> {
				if (t.getKeyNode() instanceof ScalarNode keyNode
						&& key.equals(keyNode.getValue())) {
					return new NodeTuple(t.getKeyNode(), node);
				}
				return t;
			}).toList();
			map.setValue(newValue);
		} else {
			var value = new ArrayList<>(map.getValue());
			value.add(newTuple(key, node));
			map.setValue(value);
		}

	}

	public static void removeKey(Optional<MappingNode> map, String key) {
		List<NodeTuple> newValue = map.map(MappingNode::getValue)
				.map(List::stream)
				.orElse(Stream.empty())
				.filter(t -> {
					if (t.getKeyNode() instanceof ScalarNode keyNode
							&& key.equals(keyNode.getValue())) {
						return false;
					}
					return true;
				})
				.toList();
		map.ifPresent(mn -> mn.setValue(newValue));
	}

	public static List<Node> getYamlPath(Node root, String yamlPath) {
		if (yamlPath.startsWith("/")) {
			yamlPath = yamlPath.substring(1);
		}
		List<Node> result = List.of(root);
		for (String pathPart : yamlPath.split("/")) {
			result = result.stream()
					.map(node -> getYamlPathPartResult(node, pathPart))
					.flatMap(List::stream)
					.toList();
		}
		return result;
	}

	private static List<Node> getYamlPathPartResult(
			Node node,
			String pathPart
	) {
		var index = parseInt(pathPart);
		if (index.isPresent()) {
			return nodeAsSequence(node).map(SequenceNode::getValue)
					.map(nodes -> nodes.get(index.orElseThrow()))
					.stream()
					.toList();
		}

		if (node instanceof SequenceNode sequenceNode) {
			return sequenceNode.getValue()
					.stream()
					.map(
							sequenceItem -> getYamlPathPartResult(
									sequenceItem,
									pathPart
							)
					)
					.flatMap(List::stream)
					.toList();
		}

		Matcher attributeSelectorMatcher = YAML_PATH_ATTRIBUTE_SELECTOR_PATTERN
				.matcher(pathPart);
		if (attributeSelectorMatcher.matches()) {
			var key = attributeSelectorMatcher.group(1);
			var attributeKey = attributeSelectorMatcher.group(2);
			var attributeValue = attributeSelectorMatcher.group(3);
			var attributeNode = getKeyAsNode(nodeAsMap(node), key);
			return attributeNode.stream().flatMap(n -> {
				if (n instanceof SequenceNode sequenceNode) {
					return sequenceNode.getValue().stream();
				}
				return Stream.of(n);
			})
					.filter(
							map -> getKeyAsScalar(nodeAsMap(map), attributeKey)
									.filter(
											scalar -> attributeValue
													.equals(scalar.getValue())
									)
									.isPresent()
					)
					.toList();
		}

		return getKeyAsNode(nodeAsMap(node), pathPart).stream().toList();
	}

	private static Optional<Integer> parseInt(String string) {
		try {
			return Optional.of(Integer.parseInt(string));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

}
