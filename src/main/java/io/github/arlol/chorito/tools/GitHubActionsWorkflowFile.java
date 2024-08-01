package io.github.arlol.chorito.tools;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
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

public class GitHubActionsWorkflowFile {

	private static Optional<MappingNode> getKeyAsMap(
			MappingNode map,
			String key
	) {
		return getKeyAsMap(Optional.of(map), key);
	}

	private static Optional<MappingNode> getKeyAsMap(
			Optional<MappingNode> map,
			String key
	) {
		return nodeAsMap(getKeyAsNode(map, key));
	}

	private static Optional<SequenceNode> getKeyAsSequence(
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

	private static Optional<ScalarNode> getKeyAsScalar(
			Optional<MappingNode> map,
			String key
	) {
		return nodeAsScalar(getKeyAsNode(map, key));
	}

	private static Optional<Node> getKeyAsNode(MappingNode map, String key) {
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

	private static String scalarValue(Node node) {
		return scalarValue(Optional.of(node)).orElseThrow();
	}

	private static Optional<String> scalarValue(Optional<Node> node) {
		return nodeAsScalar(node).map(ScalarNode::getValue);
	}

	public static ScalarNode nodeAsScalar(Node node) {
		return nodeAsScalar(Optional.of(node)).orElseThrow();
	}

	private static Optional<ScalarNode> nodeAsScalar(Optional<Node> node) {
		return node.filter(n -> n instanceof ScalarNode)
				.map(n -> (ScalarNode) n);
	}

	private static MappingNode nodeAsMap(Node node) {
		return nodeAsMap(Optional.of(node)).orElseThrow();
	}

	private static Optional<MappingNode> nodeAsMap(Optional<Node> node) {
		return node.filter(n -> n instanceof MappingNode)
				.map(n -> (MappingNode) n);
	}

	private static Optional<SequenceNode> nodeAsSequence(Optional<Node> node) {
		return node.filter(n -> n instanceof SequenceNode)
				.map(n -> (SequenceNode) n);
	}

	private static Consumer<? super MappingNode> copyValue(
			Optional<MappingNode> template
	) {
		return node -> template
				.ifPresent(value -> node.setValue(value.getValue()));
	}

	private static void setKey(
			Optional<MappingNode> map,
			String key,
			Node node
	) {
		List<NodeTuple> newValue = map.map(MappingNode::getValue)
				.map(List::stream)
				.orElse(Stream.empty())
				.map(t -> {
					if (t.getKeyNode() instanceof ScalarNode keyNode
							&& key.equals(keyNode.getValue())) {
						return new NodeTuple(t.getKeyNode(), node);
					}
					return t;
				})
				.toList();
		map.ifPresent(mn -> mn.setValue(newValue));
	}

	private static void removeKey(Optional<MappingNode> map, String key) {
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

	private Optional<Node> root;

	public GitHubActionsWorkflowFile(String content) {
		LoadSettings loadSettings = LoadSettings.builder()
				.setParseComments(true)
				.build();
		root = new Composer(
				loadSettings,
				new ParserImpl(
						loadSettings,
						new StreamReader(loadSettings, content)
				)
		).getSingleNode();
	}

	public String asString() {
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

	public Optional<MappingNode> getJobs() {
		return getKeyAsMap(nodeAsMap(root), "jobs");
	}

	public Optional<MappingNode> getJob(String name) {
		return getKeyAsMap(getJobs(), name);
	}

	public boolean hasJob(String name) {
		return getJob(name).isPresent();
	}

	public void setJob(String name, Optional<MappingNode> debugJob) {
		getJob(name).ifPresent(copyValue(debugJob));
	}

	public Optional<MappingNode> getOn() {
		return getKeyAsMap(nodeAsMap(root), "on");
	}

	public void setOn(Optional<MappingNode> newOn) {
		getOn().ifPresent(copyValue(newOn));
	}

	public Optional<SequenceNode> getOnSchedule() {
		return getKeyAsSequence(getOn(), "schedule");
	}

	public void setOnScheduleCron(String newCron) {
		var onSchedule = getOnSchedule();
		var firstScheduleNode = nodeAsMap(
				onSchedule.map(SequenceNode::getValue).map(l -> l.get(0))
		);
		var tuple = getKeyAsTuple(firstScheduleNode, "cron").orElseThrow();
		var scalarNode = new ScalarNode(
				Tag.STR,
				newCron,
				ScalarStyle.SINGLE_QUOTED
		);
		NodeTuple nodeTuple = new NodeTuple(tuple.getKeyNode(), scalarNode);
		firstScheduleNode.orElseThrow().setValue(List.of(nodeTuple));
	}

	public Optional<MappingNode> getEnv() {
		return getKeyAsMap(nodeAsMap(root), "env");
	}

	public void setEnv(Optional<MappingNode> newEnv) {
		getEnv().ifPresent(copyValue(newEnv));
	}

	public void updatePermissionsFromTemplate(
			GitHubActionsWorkflowFile template
	) {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			String jobName = scalarValue(jobTuple.getKeyNode());
			var job = nodeAsMap(jobTuple.getValueNode());
			if (template.hasJob(jobName)) {
				var templatePermissions = getKeyAsMap(
						template.getJob(jobName),
						"permissions"
				);
				if (templatePermissions.isPresent()) {
					Optional<MappingNode> permissions = getKeyAsMap(
							job,
							"permissions"
					);
					permissions.ifPresent(copyValue(templatePermissions));

					if (permissions.isEmpty()) {
						var permissionsKeyNode = new ScalarNode(
								Tag.STR,
								"permissions",
								ScalarStyle.PLAIN
						);
						var permissionsTuple = new NodeTuple(
								permissionsKeyNode,
								templatePermissions.orElseThrow()
						);

						int index = 0;
						for (; index < job.getValue().size(); index++) {
							NodeTuple jobDetailsTuple = job.getValue()
									.get(index);
							String detailKey = scalarValue(
									jobDetailsTuple.getKeyNode()
							);
							if ("runs-on".equals(detailKey)) {
								continue;
							}
							if ("if".equals(detailKey)) {
								continue;
							}
							if ("needs".equals(detailKey)) {
								continue;
							}
							break;
						}
						job.getValue().add(index, permissionsTuple);
					}
				}
			}
		}
	}

	public void removeEnv() {
		removeKey(nodeAsMap(root), "env");
	}

	public void removeActionFromJob(String jobName, String actionName) {
		var jobNode = getJob(jobName);
		List<Node> nodes = getKeyAsSequence(jobNode, "steps")
				.map(SequenceNode::getValue)
				.orElse(List.of())
				.stream()
				.filter(step -> {
					return scalarValue(getKeyAsNode(nodeAsMap(step), "uses"))
							.filter(uses -> uses.startsWith(actionName))
							.isEmpty();
				})
				.toList();
		setKey(
				jobNode,
				"steps",
				new SequenceNode(Tag.SEQ, nodes, FlowStyle.BLOCK)
		);
	}

	public void setKeyToSequence(String yamlpath, List<String> sequence) {
		List<String> split = Arrays.asList(yamlpath.split("\\."));
		Optional<MappingNode> parentNode = nodeAsMap(root);
		for (int i = 0; i < split.size() - 1; i++) {
			String key = split.get(i);
			parentNode = getKeyAsMap(parentNode, key);
		}
		List<Node> nodes = sequence.stream().map(value -> {
			return (Node) new ScalarNode(Tag.STR, value, ScalarStyle.PLAIN);
		}).toList();
		setKey(
				parentNode,
				split.get(split.size() - 1),
				new SequenceNode(Tag.SEQ, nodes, FlowStyle.BLOCK)
		);
	}

}
