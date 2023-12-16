package io.github.arlol.chorito.tools;

import java.io.StringWriter;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.api.StreamDataWriter;
import org.snakeyaml.engine.v2.composer.Composer;
import org.snakeyaml.engine.v2.emitter.Emitter;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.parser.ParserImpl;
import org.snakeyaml.engine.v2.scanner.StreamReader;
import org.snakeyaml.engine.v2.serializer.Serializer;

public class GitHubActionsWorkflowFile {

	private static MappingNode getKeyAsMap(MappingNode map, String key) {
		if (map == null) {
			return null;
		}
		Node orElse = map.getValue().stream().filter(t -> {
			if (t.getKeyNode() instanceof ScalarNode keyNode
					&& key.equals(keyNode.getValue())) {
				return true;
			}
			return false;
		}).map(NodeTuple::getValueNode).findFirst().orElse(null);
		return objectToMap(orElse);
	}

	private static MappingNode objectToMap(Node node) {
		if (node == null) {
			return null;
		}
		if (node instanceof MappingNode) {
			return (MappingNode) node;
		}
		throw new IllegalArgumentException("Not a map");
	}

	private Node root;

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
		).getSingleNode().orElse(null);
	}

	public String asString() {
		if (root == null) {
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
		serializer.serializeDocument(root);
		String string = writer.toString();
		string = string.replaceAll("\n\s+\n", "\n\n");
		if (string.endsWith("\n")) {
			return string;
		}
		return string + "\n";
	}

	public MappingNode getJobs() {
		if (root instanceof MappingNode mappingNode) {
			return getKeyAsMap(mappingNode, "jobs");
		}
		return null;
	}

	public MappingNode getJob(String name) {
		return getKeyAsMap(getJobs(), name);
	}

	public boolean hasJob(String name) {
		return getJob(name) != null;
	}

	public void setJob(String name, MappingNode newJob) {
		MappingNode job = getJob(name);
		if (job != null) {
			job.setValue(newJob.getValue());
		}
	}

}

class StreamToStringWriter extends StringWriter implements StreamDataWriter {

}
