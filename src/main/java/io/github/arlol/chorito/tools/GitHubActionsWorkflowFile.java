package io.github.arlol.chorito.tools;

import static io.github.arlol.chorito.tools.Yamls.copyValue;
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
import static io.github.arlol.chorito.tools.Yamls.removeKey;
import static io.github.arlol.chorito.tools.Yamls.scalarValue;
import static io.github.arlol.chorito.tools.Yamls.setKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.comments.CommentLine;
import org.snakeyaml.engine.v2.comments.CommentType;
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

	public static String removeVersions(String input) {
		return input.replaceAll("(?m)^([\\s-]*uses:.*?)@.*$", "$1@\n");
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

	public GitHubActionsWorkflowFile copy() {
		return new GitHubActionsWorkflowFile(asString());
	}

	public String asStringWithoutVersions() {
		return removeVersions(asString());
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

	public Optional<String> getOnScheduleCron() {
		return scalarValue(
				getYamlPath(root.orElseThrow(), "/on/schedule/0/cron").stream()
						.findFirst()
		);
	}

	public void setOnScheduleCron(String newCron) {
		setKey(
				nodeAsMap(
						getYamlPath(root.orElseThrow(), "/on/schedule/0")
								.stream()
								.findFirst()
								.orElseThrow()
				),
				"cron",
				newScalar(newCron, ScalarStyle.DOUBLE_QUOTED)
		);
	}

	public Optional<MappingNode> getEnv() {
		return getKeyAsMap(nodeAsMap(root), "env");
	}

	public void setEnv(Optional<MappingNode> newEnv) {
		getEnv().ifPresent(copyValue(newEnv));
	}

	public void removeEnv(String key) {
		removeKey(getEnv(), key);
		getEnv().ifPresent(env -> {
			if (env.getValue().isEmpty()) {
				removeEnv();
			}
		});
	}

	public void updatePermissionsFromTemplate(
			GitHubActionsWorkflowFile template
	) {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			String jobName = scalarValue(jobTuple.getKeyNode()).orElseThrow();
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
							).orElseThrow();
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
		setKey(jobNode.orElseThrow(), "steps", newSequence(nodes));
	}

	public void setJobMatrixKey(String job, String key, List<String> values) {
		List<Node> nodes = new ArrayList<>();
		values.stream()
				.map(value -> newScalar(value, ScalarStyle.PLAIN))
				.forEach(scalar -> nodes.add(scalar));
		setKey(
				nodeAsMap(
						getYamlPath(
								getJob(job).orElseThrow(),
								"/strategy/matrix"
						)
				).getFirst(),
				key,
				newSequence(nodes)
		);
	}

	public void actionsCheckoutWithPersistCredentials() {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			var jobNode = nodeAsMap(jobTuple.getValueNode());

			List<Node> steps = getKeyAsSequence(jobNode, "steps")
					.map(SequenceNode::getValue)
					.orElse(List.of())
					.stream()
					.map(step -> {
						var stepNode = nodeAsMap(step);
						if (scalarValue(getKeyAsNode(stepNode, "uses"))
								.filter(
										uses -> uses
												.startsWith("actions/checkout@")
								)
								.isPresent()) {
							var withNode = getKeyAsMap(stepNode, "with")
									.orElse(newMap());
							var persistCredentialsNode = getKeyAsNode(
									withNode,
									"persist-credentials"
							).orElse(newScalar(false));
							setKey(
									withNode,
									"persist-credentials",
									persistCredentialsNode
							);
							setKey(stepNode, "with", withNode);
						}
						return step;
					})
					.toList();
			setKey(jobNode, "steps", newSequence(steps));
		}
	}

	public void removeInputParameterFromAction(
			String actionName,
			String inputParameter
	) {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			var jobNode = nodeAsMap(jobTuple.getValueNode());

			List<Node> steps = getKeyAsSequence(jobNode, "steps")
					.map(SequenceNode::getValue)
					.orElse(List.of())
					.stream()
					.peek(step -> {
						var stepNode = nodeAsMap(step);
						if (scalarValue(getKeyAsNode(stepNode, "uses"))
								.filter(
										uses -> uses
												.startsWith(actionName + "@")
								)
								.isPresent()) {
							removeKey(
									getKeyAsMap(stepNode, "with"),
									inputParameter
							);
						}
					})
					.toList();
			setKey(jobNode, "steps", newSequence(steps));
		}
	}

	public void addInputParameterToAction(
			String actionName,
			String inputParameter,
			String value
	) {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			var jobNode = nodeAsMap(jobTuple.getValueNode());

			List<Node> steps = getKeyAsSequence(jobNode, "steps")
					.map(SequenceNode::getValue)
					.orElse(List.of())
					.stream()
					.peek(step -> {
						var stepNode = nodeAsMap(step);
						if (scalarValue(getKeyAsNode(stepNode, "uses"))
								.filter(
										uses -> uses
												.startsWith(actionName + "@")
								)
								.isPresent()) {
							var withNode = getKeyAsMap(stepNode, "with")
									.orElseGet(() -> {
										var with = newMap();
										setKey(stepNode, "with", with);
										return with;
									});
							setKey(withNode, inputParameter, newScalar(value));
						}
					})
					.toList();
			setKey(jobNode, "steps", newSequence(steps));
		}
	}

	public void clearPermissions() {
		nodeAsMap(root).ifPresent(mappingNode -> {
			setKey(mappingNode, "permissions", newMap());
		});
	}

	public void singleToDoubleQuote() {
		root.ifPresent(this::singleToDoubleQuote);
	}

	public Node singleToDoubleQuote(Node node) {
		return switch (node) {
		case MappingNode mappingNode -> {
			var value = mappingNode.getValue().stream().map(nodeTuple -> {
				return newTuple(
						nodeTuple.getKeyNode(),
						singleToDoubleQuote(nodeTuple.getValueNode())
				);
			}).toList();
			mappingNode.setValue(value);
			yield mappingNode;
		}
		case SequenceNode sequenceNode -> newSequence(
				sequenceNode.getValue()
						.stream()
						.map(this::singleToDoubleQuote)
						.toList()
		);
		case ScalarNode scalarNode -> {
			String value = scalarNode.getValue();

			if (scalarNode.getScalarStyle() == ScalarStyle.SINGLE_QUOTED) {
				if (!value.contains("\"")) {
					scalarNode = new ScalarNode(
							scalarNode.getTag(),
							value,
							ScalarStyle.DOUBLE_QUOTED
					);
				}
			}

			if (scalarNode.getScalarStyle() == ScalarStyle.DOUBLE_QUOTED) {

				if ((value.equalsIgnoreCase("off")
						|| value.equalsIgnoreCase("on")) || value.contains("*")
						|| value.contains(":")) {
					scalarNode = new ScalarNode(
							scalarNode.getTag(),
							value,
							ScalarStyle.DOUBLE_QUOTED
					);
				} else {
					scalarNode = new ScalarNode(
							scalarNode.getTag(),
							value,
							ScalarStyle.PLAIN
					);
				}
			}

			yield scalarNode;
		}
		default -> {
			yield node;
		}
		};
	}

	public void sortKeys() {
		nodeAsMap(root).ifPresent(mappingNode -> {
			var workflowList = mappingNode.getValue()
					.stream()
					.sorted(Comparator.comparingInt(tuple -> {
						if (tuple.getKeyNode() instanceof ScalarNode keyNode) {
							return switch (keyNode.getValue()) {
							case "name" -> 10;
							case "on" -> 50;
							case "permissions" -> 70;
							case "env" -> 80;
							case "jobs" -> 200;
							default -> 100;
							};
						}
						return 100;
					}))
					.toList();
			mappingNode.setValue(workflowList);
		});

		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			var jobNode = nodeAsMap(jobTuple.getValueNode());

			List<Node> steps = getKeyAsSequence(jobNode, "steps")
					.map(SequenceNode::getValue)
					.orElse(List.of())
					.stream()
					.map(step -> {
						var stepNode = nodeAsMap(step);

						getKeyAsMap(stepNode, "with").ifPresent(mappingNode -> {
							var workflowList = mappingNode.getValue()
									.stream()
									.sorted(Comparator.comparing(tuple -> {
										if (tuple
												.getKeyNode() instanceof ScalarNode keyNode) {
											return keyNode.getValue();
										}
										return tuple.getKeyNode().toString();
									}))
									.toList();
							mappingNode.setValue(workflowList);
						});

						var stepList = stepNode.getValue()
								.stream()
								.sorted(Comparator.comparingInt(tuple -> {
									if (tuple
											.getKeyNode() instanceof ScalarNode keyNode) {
										return switch (keyNode.getValue()) {
										case "name" -> 10;
										case "id" -> 11;
										case "if" -> 12;
										case "uses" -> 20;
										case "with" -> 2000;
										case "run" -> 2000;
										default -> 1000;
										};
									}
									return 1000;
								}))
								.toList();
						stepNode.setValue(stepList);
						return step;
					})
					.toList();
			setKey(jobNode, "steps", newSequence(steps));

			var jobList = jobNode.getValue()
					.stream()
					.sorted(Comparator.comparingInt(tuple -> {
						if (tuple.getKeyNode() instanceof ScalarNode keyNode) {
							return switch (keyNode.getValue()) {
							case "name" -> 10;
							case "needs" -> 15;
							case "if" -> 20;
							case "strategy" -> 30;
							case "runs-on" -> 50;
							case "environment" -> 60;
							case "timeout-minutes" -> 70;
							case "permissions" -> 80;
							case "outputs" -> 90;
							case "steps" -> 2000;
							default -> 1000;
							};
						}
						return 1000;
					}))
					.toList();
			jobNode.setValue(jobList);
		}
	}

	public void replaceActionWith(
			String oldAction,
			String newActionRef,
			String newActionVersion
	) {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			var jobNode = nodeAsMap(jobTuple.getValueNode());

			List<Node> steps = getKeyAsSequence(jobNode, "steps")
					.map(SequenceNode::getValue)
					.orElse(List.of())
					.stream()
					.peek(step -> {
						var stepNode = nodeAsMap(step);
						getKeyAsScalar(stepNode, "uses")
								.filter(
										uses -> uses.getValue()
												.startsWith(oldAction + "@")
								)
								.ifPresent(uses -> {
									var scalarNode = newScalar(
											newActionRef,
											uses.getScalarStyle()
									);
									scalarNode.setInLineComments(
											List.of(
													new CommentLine(
															Optional.empty(),
															Optional.empty(),
															" " + newActionVersion,
															CommentType.IN_LINE
													)
											)
									);
									setKey(stepNode, "uses", scalarNode);
								});
					})
					.toList();
			setKey(jobNode, "steps", newSequence(steps));
		}
	}

}
