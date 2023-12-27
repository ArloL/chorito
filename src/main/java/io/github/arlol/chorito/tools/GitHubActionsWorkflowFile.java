package io.github.arlol.chorito.tools;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.StreamDataWriter;
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
		return objectToMap(getKeyAsNode(map, key));
	}

	private static Optional<SequenceNode> getKeyAsSequence(
			Optional<MappingNode> map,
			String key
	) {
		return objectToSequence(getKeyAsNode(map, key));
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
		return nodeToScalar(node).getValue();
	}

	private static ScalarNode nodeToScalar(Node node) {
		return objectToScalar(Optional.of(node)).orElseThrow();
	}

	private static Optional<ScalarNode> objectToScalar(Optional<Node> node) {
		return node.filter(n -> n instanceof ScalarNode)
				.map(n -> (ScalarNode) n);
	}

	private static MappingNode objectToMap(Node node) {
		return objectToMap(Optional.of(node)).orElseThrow();
	}

	private static Optional<MappingNode> objectToMap(Optional<Node> node) {
		return node.filter(n -> n instanceof MappingNode)
				.map(n -> (MappingNode) n);
	}

	private static Optional<SequenceNode> objectToSequence(
			Optional<Node> node
	) {
		return node.filter(n -> n instanceof SequenceNode)
				.map(n -> (SequenceNode) n);
	}

	private static Consumer<? super MappingNode> copyValue(
			Optional<MappingNode> template
	) {
		return node -> node.setValue(template.orElseThrow().getValue());
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

		StreamToStringWriter writer = new StreamToStringWriter();
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
		return getKeyAsMap(objectToMap(root), "jobs");
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
		return getKeyAsMap(objectToMap(root), "on");
	}

	public void setOn(Optional<MappingNode> newOn) {
		getOn().ifPresent(copyValue(newOn));
	}

	public Optional<SequenceNode> getOnSchedule() {
		return getKeyAsSequence(getOn(), "schedule");
	}

	public void setOnScheduleCron(String newCron) {
		var onSchedule = getOnSchedule();
		var firstScheduleNode = objectToMap(
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
		return getKeyAsMap(objectToMap(root), "env");
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
			var job = objectToMap(jobTuple.getValueNode());
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

}

class StreamToStringWriter extends StringWriter implements StreamDataWriter {

}
