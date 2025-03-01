package io.github.arlol.chorito.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
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
		return new Composer(
				loadSettings,
				new ParserImpl(
						loadSettings,
						new StreamReader(loadSettings, content)
				)
		).getSingleNode();
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

	public static Consumer<? super MappingNode> copyValue(
			Optional<MappingNode> template
	) {
		return node -> template
				.ifPresent(value -> node.setValue(value.getValue()));
	}

	@SuppressWarnings("null") // null analysis is unnecessarily complicated
	public static void setKey(MappingNode map, String key, Node node) {
		var newValue = map.getValue().stream().map(t -> {
			if (t.getKeyNode() instanceof ScalarNode keyNode
					&& key.equals(keyNode.getValue())) {
				return new NodeTuple(t.getKeyNode(), node);
			}
			return t;
		}).toList();
		map.setValue(newValue);
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
