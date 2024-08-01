package io.github.arlol.chorito.tools;

import static io.github.arlol.chorito.tools.Yamls.getKeyAsNode;
import static io.github.arlol.chorito.tools.Yamls.getKeyAsSequence;
import static io.github.arlol.chorito.tools.Yamls.newMap;
import static io.github.arlol.chorito.tools.Yamls.newSequence;
import static io.github.arlol.chorito.tools.Yamls.newTuple;
import static io.github.arlol.chorito.tools.Yamls.nodeAsMap;
import static io.github.arlol.chorito.tools.Yamls.scalarValue;

import java.util.List;
import java.util.Optional;

import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

public class DependabotConfigFile {

	private final Optional<Node> root;

	public DependabotConfigFile() {
		root = Optional.of(
				newMap(
						newTuple("version", 2),
						newTuple("updates", newSequence())
				)
		);
	}

	public DependabotConfigFile(String content) {
		root = Yamls.load(content);
	}

	public String asString() {
		return Yamls.asString(root);
	}

	private Optional<SequenceNode> getUpdates() {
		return getKeyAsSequence(nodeAsMap(root), "updates");
	}

	public boolean hasEcosystemWithDirectory(
			String packageEcosystem,
			String directory
	) {
		return getUpdates().map(SequenceNode::getValue)
				.orElse(List.of())
				.stream()
				.map(node -> nodeAsMap(node))
				.anyMatch(step -> {
					return scalarValue(getKeyAsNode(step, "package-ecosystem"))
							.filter(value -> packageEcosystem.equals(value))
							.isPresent()
							&& scalarValue(getKeyAsNode(step, "directory"))
									.filter(value -> directory.equals(value))
									.isPresent();
				});
	}

	public void addEcosystemInDirectory(String ecosystem, String directory) {
		if (hasEcosystemWithDirectory(ecosystem, directory)) {
			return;
		}

		var nodes = List.of(
				newTuple("package-ecosystem", ecosystem),
				newTuple("directory", directory),
				newTuple("schedule", newMap(newTuple("interval", "daily")))
		);
		var updates = getUpdates();
		updates.map(SequenceNode::getValue)
				.ifPresentOrElse(list -> list.add(newMap(nodes)), () -> {

				});

	}

}
