package io.github.arlol.chorito.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

	private Yamls() {
	}

	public static Optional<SequenceNode> getKeyAsSequence(
			Optional<MappingNode> map,
			String key
	) {
		return nodeAsSequence(getKeyAsNode(map, key));
	}

	private static Optional<ScalarNode> getKeyAsScalar(
			MappingNode map,
			String key
	) {
		return getKeyAsScalar(Optional.of(map), key);
	}

	private static Optional<ScalarNode> getKeyAsScalar(
			Optional<MappingNode> map,
			String key
	) {
		return nodeAsScalar(getKeyAsNode(map, key));
	}

	public static Optional<Node> getKeyAsNode(MappingNode map, String key) {
		return getKeyAsNode(Optional.of(map), key);
	}

	private static Optional<Node> getKeyAsNode(
			Optional<MappingNode> map,
			String key
	) {
		return getKeyAsTuple(map, key).map(NodeTuple::getValueNode);
	}

	private static Optional<NodeTuple> getKeyAsTuple(
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

	private static ScalarNode nodeAsScalar(Node node) {
		return nodeAsScalar(Optional.of(node)).orElseThrow();
	}

	private static Optional<ScalarNode> nodeAsScalar(Optional<Node> node) {
		return node.filter(n -> n instanceof ScalarNode)
				.map(n -> (ScalarNode) n);
	}

	public static MappingNode nodeAsMap(Node node) {
		return nodeAsMap(Optional.of(node)).orElseThrow();
	}

	public static Optional<MappingNode> nodeAsMap(Optional<Node> node) {
		return node.filter(n -> n instanceof MappingNode)
				.map(n -> (MappingNode) n);
	}

	private static Optional<SequenceNode> nodeAsSequence(Optional<Node> node) {
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

	public static NodeTuple newTuple(String key, int value) {
		return newTuple(
				key,
				new ScalarNode(
						Tag.INT,
						String.valueOf(value),
						ScalarStyle.PLAIN
				)
		);
	}

	public static NodeTuple newTuple(String key, String value) {
		return newTuple(key, value, ScalarStyle.DOUBLE_QUOTED);
	}

	public static NodeTuple newTuple(
			String key,
			String value,
			ScalarStyle style
	) {
		return newTuple(key, new ScalarNode(Tag.STR, value, style));
	}

	public static NodeTuple newTuple(String key, Node valueNode) {
		var keyNode = new ScalarNode(Tag.STR, key, ScalarStyle.PLAIN);
		return new NodeTuple(keyNode, valueNode);
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

}
