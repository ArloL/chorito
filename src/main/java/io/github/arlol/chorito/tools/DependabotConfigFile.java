package io.github.arlol.chorito.tools;

import static io.github.arlol.chorito.tools.Yamls.getKeyAsMap;
import static io.github.arlol.chorito.tools.Yamls.getKeyAsNode;
import static io.github.arlol.chorito.tools.Yamls.getKeyAsScalar;
import static io.github.arlol.chorito.tools.Yamls.getKeyAsSequence;
import static io.github.arlol.chorito.tools.Yamls.getYamlPath;
import static io.github.arlol.chorito.tools.Yamls.newMap;
import static io.github.arlol.chorito.tools.Yamls.newScalar;
import static io.github.arlol.chorito.tools.Yamls.newSequence;
import static io.github.arlol.chorito.tools.Yamls.newTuple;
import static io.github.arlol.chorito.tools.Yamls.nodeAsMap;
import static io.github.arlol.chorito.tools.Yamls.scalarValue;
import static io.github.arlol.chorito.tools.Yamls.setKey;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.SequenceNode;

public class DependabotConfigFile {

	private final Optional<Node> root;

	public DependabotConfigFile() {
		root = Optional.of(
				newMap(
						newTuple("version", newScalar(2)),
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

	private List<Node> getUpdates() {
		return getKeyAsSequence(nodeAsMap(root), "updates")
				.map(SequenceNode::getValue)
				.orElseThrow();
	}

	private Stream<MappingNode> getUpdatesAsMappingNode() {
		return getUpdates().stream().map(Yamls::nodeAsMap);
	}

	public boolean hasEcosystemWithDirectory(
			String packageEcosystem,
			String directory
	) {
		var directoryWithoutTrailingSlash = directory
				.substring(0, directory.length() - 1);
		return getUpdatesAsMappingNode().anyMatch(step -> {
			return scalarValue(getKeyAsNode(step, "package-ecosystem"))
					.filter(packageEcosystem::equals)
					.isPresent()
					&& scalarValue(getKeyAsNode(step, "directory"))
							.filter(
									value -> directory.equals(value)
											|| directoryWithoutTrailingSlash
													.equals(value)
							)
							.isPresent();
		});
	}

	public void addEcosystemInDirectory(String ecosystem, String directory) {
		if (hasEcosystemWithDirectory(ecosystem, directory)) {
			return;
		}

		var nodes = List.of(
				newTuple("package-ecosystem", newScalar(ecosystem)),
				newTuple("directory", newScalar(directory)),
				newTuple(
						"schedule",
						newMap(newTuple("interval", newScalar("monthly")))
				)
		);
		getUpdates().add(newMap(nodes));
	}

	public void changeDailyScheduleToMonthly() {
		getYamlPath(root.orElseThrow(), "/updates/schedule[interval=daily]")
				.forEach(node -> {
					setKey(nodeAsMap(node), "interval", newScalar("monthly"));
				});
	}

	public void addCooldownIfMissing() {
		getUpdatesAsMappingNode().forEach(update -> {
			getKeyAsMap(update, "cooldown").ifPresentOrElse(cooldown -> {
				getKeyAsScalar(cooldown, "default-days")
						.ifPresentOrElse(defaultDays -> {
							if (Integer.parseInt(defaultDays.getValue()) < 7) {
								setKey(cooldown, "default-days", newScalar(7));
							}
						}, () -> {
							setKey(cooldown, "default-days", newScalar(7));
						});
			}, () -> {
				setKey(
						update,
						"cooldown",
						newMap(newTuple("default-days", newScalar(7)))
				);
			});
		});
	}

}
