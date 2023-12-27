package io.github.arlol.chorito.tools;

import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.StreamDataWriter;
import org.snakeyaml.engine.v2.composer.Composer;
import org.snakeyaml.engine.v2.emitter.Emitter;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.scanner.StreamReader;
import org.snakeyaml.engine.v2.serializer.Serializer;

public class GitHubActionsWorkflowFile {

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

	private Optional<ScalarNode> getKeyAsScalar(
			Optional<MappingNode> map,
			String key
	) {
		return objectToScalar(getKeyAsNode(map, key));
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

	private static Optional<ScalarNode> objectToScalar(Optional<Node> node) {
		return node.filter(n -> n instanceof ScalarNode)
				.map(n -> (ScalarNode) n);
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
		getJob(name).ifPresent(
				job -> job.setValue(debugJob.orElseThrow().getValue())
		);
	}

	public Optional<MappingNode> getOn() {
		return getKeyAsMap(objectToMap(root), "on");
	}

	public void setOn(Optional<MappingNode> newOn) {
		getOn().ifPresent(on -> on.setValue(newOn.orElseThrow().getValue()));
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
		var currentCronNode = getKeyAsScalar(firstScheduleNode, "cron")
				.orElseThrow();
		var scalarNode = new ScalarNode(
				currentCronNode.getTag(),
				newCron,
				currentCronNode.getScalarStyle()
		);
		NodeTuple nodeTuple = new NodeTuple(tuple.getKeyNode(), scalarNode);
		firstScheduleNode.orElseThrow().setValue(List.of(nodeTuple));

		System.out.println();
	}

	public Optional<MappingNode> getEnv() {
		return getKeyAsMap(objectToMap(root), "env");
	}

	public void setEnv(Optional<MappingNode> newEnv) {
		getEnv().ifPresent(
				env -> env.setValue(newEnv.orElseThrow().getValue())
		);
	}

}

class StreamToStringWriter extends StringWriter implements StreamDataWriter {

}
