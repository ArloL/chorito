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
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

import org.snakeyaml.engine.v2.api.DumpSettings;
import org.snakeyaml.engine.v2.comments.CommentLine;
import org.snakeyaml.engine.v2.comments.CommentType;
import org.snakeyaml.engine.v2.common.ScalarStyle;
import org.snakeyaml.engine.v2.emitter.Emitter;
import org.snakeyaml.engine.v2.nodes.MappingNode;
import org.snakeyaml.engine.v2.nodes.Node;
import org.snakeyaml.engine.v2.nodes.NodeTuple;
import org.snakeyaml.engine.v2.nodes.ScalarNode;
import org.snakeyaml.engine.v2.nodes.SequenceNode;
import org.snakeyaml.engine.v2.nodes.Tag;
import org.snakeyaml.engine.v2.serializer.Serializer;

public class GitHubActionsWorkflowFile {

	private static final String STEPS = "steps";
	private static final String PERMISSIONS = "permissions";
	private static final String SETUP_JAVA_ACTION = "actions/setup-java";
	private static final String DISTRIBUTION_TEMURIN = "temurin";
	private static final String JAVA_VERSION = "java-version";
	private static final String RENOVATE_JAVA_VERSION_COMMENT = "renovate: datasource=java-version depName=java";
	private static final Pattern USES_VERSION = Pattern
			.compile("(?m)^([ \\t-]*uses:[^@\\n]*)@[^\\n]*");

	public static String removeVersions(String input) {
		return USES_VERSION.matcher(input).replaceAll("$1@\n");
	}

	private Optional<Node> root;

	public GitHubActionsWorkflowFile(String content) {
		root = Yamls.load(content);
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

	/**
	 * The template states the permissions a job needs, not the only ones it may
	 * have. A job that grants itself more -- a release attesting its assets
	 * needs attestations and id-token on top of contents -- keeps them, so the
	 * next chores run does not quietly take away what someone added on purpose.
	 * The price is that chorito cannot withdraw a permission once a job has it.
	 */
	private static void applyPermissions(
			MappingNode job,
			Optional<MappingNode> templatePermissions
	) {
		Optional<MappingNode> permissions = getKeyAsMap(job, PERMISSIONS);
		if (permissions.isPresent()) {
			var merged = new ArrayList<>(permissions.orElseThrow().getValue());
			for (NodeTuple required : templatePermissions
					.map(MappingNode::getValue)
					.orElse(List.of())) {
				String key = scalarValue(required.getKeyNode()).orElseThrow();
				merged.removeIf(
						t -> scalarValue(t.getKeyNode()).filter(key::equals)
								.isPresent()
				);
				merged.add(required);
			}
			sortTuples(
					permissions.orElseThrow(),
					merged,
					Comparator.comparing(
							t -> scalarValue(t.getKeyNode()).orElse("")
					)
			);
			return;
		}
		var permissionsTuple = new NodeTuple(
				new ScalarNode(Tag.STR, PERMISSIONS, ScalarStyle.PLAIN),
				templatePermissions.orElseThrow()
		);
		job.getValue().add(permissionsInsertionIndex(job), permissionsTuple);
	}

	/**
	 * Permissions belong after the keys that introduce a job, so this skips
	 * over them and stops at the first other key.
	 */
	private static int permissionsInsertionIndex(MappingNode job) {
		List<String> jobIntroduction = List.of("runs-on", "if", "needs");
		int index = 0;
		for (; index < job.getValue().size(); index++) {
			String detailKey = scalarValue(
					job.getValue().get(index).getKeyNode()
			).orElseThrow();
			if (!jobIntroduction.contains(detailKey)) {
				break;
			}
		}
		return index;
	}

	public void removeEnv() {
		removeKey(nodeAsMap(root), "env");
	}

	public void removeActionFromJob(String jobName, String actionName) {
		var jobNode = getJob(jobName);
		getKeyAsSequence(jobNode, STEPS).ifPresent(stepsNode -> {
			List<Node> nodes = stepsNode.getValue().stream().filter(step -> {
				return scalarValue(getKeyAsNode(nodeAsMap(step), "uses"))
						.filter(uses -> uses.startsWith(actionName))
						.isEmpty();
			}).toList();
			setKey(jobNode.orElseThrow(), STEPS, newSequence(nodes));
		});
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

			getKeyAsSequence(jobNode, STEPS).ifPresent(stepsNode -> {
				List<Node> steps = stepsNode.getValue().stream().map(step -> {
					var stepNode = nodeAsMap(step);
					if (scalarValue(getKeyAsNode(stepNode, "uses"))
							.filter(
									uses -> uses.startsWith("actions/checkout@")
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
				}).toList();
				setKey(jobNode, STEPS, newSequence(steps));
			});
		}
	}

	public void removeInputParameterFromAction(
			String actionName,
			String inputParameter
	) {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			var jobNode = nodeAsMap(jobTuple.getValueNode());

			getKeyAsSequence(jobNode, STEPS).ifPresent(stepsNode -> {
				List<Node> steps = stepsNode.getValue().stream().peek(step -> {
					var stepNode = nodeAsMap(step);
					if (scalarValue(getKeyAsNode(stepNode, "uses"))
							.filter(uses -> uses.startsWith(actionName + "@"))
							.isPresent()) {
						removeKey(
								getKeyAsMap(stepNode, "with"),
								inputParameter
						);
					}
				}).toList();
				setKey(jobNode, STEPS, newSequence(steps));
			});
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

			getKeyAsSequence(jobNode, STEPS).ifPresent(stepsNode -> {
				List<Node> steps = stepsNode.getValue().stream().peek(step -> {
					var stepNode = nodeAsMap(step);
					if (scalarValue(getKeyAsNode(stepNode, "uses"))
							.filter(uses -> uses.startsWith(actionName + "@"))
							.isPresent()) {
						var withNode = getKeyAsMap(stepNode, "with")
								.orElseGet(() -> {
									var with = newMap();
									setKey(stepNode, "with", with);
									return with;
								});
						setKey(withNode, inputParameter, newScalar(value));
					}
				}).toList();
				setKey(jobNode, STEPS, newSequence(steps));
			});
		}
	}

	/**
	 * The version a Temurin setup-java step is pinned to, if any. The chores
	 * that regenerate a workflow from a template carry it across so the bump
	 * Renovate made survives.
	 */
	public Optional<String> getPinnedJavaVersion() {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			var jobNode = nodeAsMap(jobTuple.getValueNode());
			for (Node step : getKeyAsSequence(jobNode, STEPS)
					.map(SequenceNode::getValue)
					.orElse(List.of())) {
				var with = getKeyAsMap(nodeAsMap(step), "with");
				if (scalarValue(getKeyAsNode(with, "distribution"))
						.filter(DISTRIBUTION_TEMURIN::equals)
						.isPresent()) {
					var version = scalarValue(getKeyAsNode(with, JAVA_VERSION));
					if (version.isPresent()) {
						return version;
					}
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Pins every Temurin setup-java step to {@code version}, replacing whatever
	 * the step said before. Carrying a version across belongs to the caller,
	 * which reads it with {@link #getPinnedJavaVersion()} first: a chore that
	 * rebuilds a workflow from a template must not inherit the template's own
	 * pin, and chorito's template for this file is a symlink to the workflow it
	 * runs itself.
	 */
	public void pinTemurinJavaVersion(String version) {
		forEachTemurinSetupJavaWith(with -> {
			var withNode = with.orElseThrow();
			removeKey(with, "java-version-file");
			removeKey(with, JAVA_VERSION);
			var keyNode = newScalar(JAVA_VERSION, ScalarStyle.PLAIN);
			keyNode.setBlockComments(
					List.of(
							new CommentLine(
									Optional.empty(),
									Optional.empty(),
									" " + RENOVATE_JAVA_VERSION_COMMENT,
									CommentType.BLOCK
							)
					)
			);
			var tuples = new ArrayList<>(withNode.getValue());
			tuples.add(
					new NodeTuple(
							keyNode,
							newScalar(version, ScalarStyle.PLAIN)
					)
			);
			withNode.setValue(tuples);
		});
	}

	/**
	 * Runs {@code body} against the {@code with} of every setup-java step
	 * asking for Temurin. Those are the steps that build no native image, so
	 * they are the ones whose JDK is chorito's to decide.
	 */
	private void forEachTemurinSetupJavaWith(
			Consumer<Optional<MappingNode>> body
	) {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			var jobNode = nodeAsMap(jobTuple.getValueNode());
			for (Node step : getKeyAsSequence(jobNode, STEPS)
					.map(SequenceNode::getValue)
					.orElse(List.of())) {
				var stepNode = nodeAsMap(step);
				if (scalarValue(getKeyAsNode(stepNode, "uses"))
						.filter(
								uses -> uses.startsWith(SETUP_JAVA_ACTION + "@")
						)
						.isEmpty()) {
					continue;
				}
				var with = getKeyAsMap(stepNode, "with");
				if (scalarValue(getKeyAsNode(with, "distribution"))
						.filter(DISTRIBUTION_TEMURIN::equals)
						.isEmpty()) {
					continue;
				}
				body.accept(with);
			}
		}
	}

	/**
	 * Points every Temurin setup-java step back at {@code .tool-versions},
	 * dropping a pin and the renovate comment above it.
	 */
	public void useToolVersionsFile() {
		forEachTemurinSetupJavaWith(with -> {
			removeKey(with, JAVA_VERSION);
			setKey(
					with.orElseThrow(),
					"java-version-file",
					newScalar(".tool-versions", ScalarStyle.PLAIN)
			);
		});
	}

	/** The step of {@code jobName} whose {@code name} is {@code stepName}. */
	public Optional<Node> getStepByName(String jobName, String stepName) {
		return steps(jobName).stream()
				.filter(
						step -> scalarValue(
								getKeyAsNode(nodeAsMap(step), "name")
						).filter(stepName::equals).isPresent()
				)
				.findFirst();
	}

	public boolean hasStepUsing(String jobName, String actionName) {
		return steps(jobName).stream()
				.anyMatch(
						step -> scalarValue(
								getKeyAsNode(nodeAsMap(step), "uses")
						).filter(uses -> uses.startsWith(actionName + "@"))
								.isPresent()
				);
	}

	/**
	 * Inserts {@code step} directly above the step called {@code stepName}, or
	 * does nothing when that step is not there.
	 */
	public void insertStepBefore(String jobName, String stepName, Node step) {
		var job = getJob(jobName);
		var stepsNode = getKeyAsSequence(job, STEPS);
		if (stepsNode.isEmpty()) {
			return;
		}
		List<Node> steps = new ArrayList<>(stepsNode.orElseThrow().getValue());
		for (int i = 0; i < steps.size(); i++) {
			if (scalarValue(getKeyAsNode(nodeAsMap(steps.get(i)), "name"))
					.filter(stepName::equals)
					.isPresent()) {
				steps.add(i, step);
				setKey(job.orElseThrow(), STEPS, newSequence(steps));
				return;
			}
		}
	}

	/** Whether any value anywhere in {@code jobName} contains {@code text}. */
	public boolean jobMentions(String jobName, String text) {
		return getJob(jobName)
				.map(job -> Yamls.asString(Optional.of((Node) job)))
				.filter(job -> job.contains(text))
				.isPresent();
	}

	/**
	 * Grants {@code permissions} to {@code jobName} on top of whatever it
	 * already has.
	 */
	public void grantJobPermissions(
			String jobName,
			Map<String, String> permissions
	) {
		var job = getJob(jobName);
		if (job.isEmpty()) {
			return;
		}
		var granted = newMap(
				permissions.entrySet()
						.stream()
						.map(
								e -> newTuple(
										newScalar(
												e.getKey(),
												ScalarStyle.PLAIN
										),
										newScalar(
												e.getValue(),
												ScalarStyle.PLAIN
										)
								)
						)
						.toList()
		);
		applyPermissions(job.orElseThrow(), Optional.of(granted));
	}

	private List<Node> steps(String jobName) {
		return getKeyAsSequence(getJob(jobName), STEPS)
				.map(SequenceNode::getValue)
				.orElse(List.of());
	}

	public void clearPermissions() {
		nodeAsMap(root).ifPresent(mappingNode -> {
			setKey(mappingNode, PERMISSIONS, newMap());
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

	private static final Comparator<NodeTuple> WORKFLOW_KEY_ORDER = keyOrder(
			key -> switch (key) {
			case "name" -> 10;
			case "on" -> 50;
			case PERMISSIONS -> 70;
			case "env" -> 80;
			case "jobs" -> 200;
			default -> 100;
			},
			100
	);

	private static final Comparator<NodeTuple> JOB_KEY_ORDER = keyOrder(
			key -> switch (key) {
			case "name" -> 10;
			case "needs" -> 15;
			case "if" -> 20;
			case "strategy" -> 30;
			case "runs-on" -> 50;
			case "environment" -> 60;
			case "timeout-minutes" -> 70;
			case PERMISSIONS -> 80;
			case "outputs" -> 90;
			case STEPS -> 2000;
			default -> 1000;
			},
			1000
	);

	private static final Comparator<NodeTuple> STEP_KEY_ORDER = keyOrder(
			key -> switch (key) {
			case "name" -> 10;
			case "id" -> 11;
			case "if" -> 12;
			case "uses" -> 20;
			case "with" -> 2000;
			case "run" -> 2000;
			default -> 1000;
			},
			1000
	);

	/**
	 * Orders tuples by the rank of their key, with {@code fallback} for the
	 * keys that are not plain scalars.
	 */
	private static Comparator<NodeTuple> keyOrder(
			ToIntFunction<String> ranking,
			int fallback
	) {
		return Comparator.comparingInt(tuple -> {
			if (tuple.getKeyNode() instanceof ScalarNode keyNode) {
				return ranking.applyAsInt(keyNode.getValue());
			}
			return fallback;
		});
	}

	public void sortKeys() {
		nodeAsMap(root).ifPresent(
				workflow -> sortTuples(workflow, WORKFLOW_KEY_ORDER)
		);
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			sortJobKeys(nodeAsMap(jobTuple.getValueNode()));
		}
	}

	private static void sortJobKeys(MappingNode jobNode) {
		getKeyAsSequence(jobNode, STEPS).ifPresent(stepsNode -> {
			List<Node> steps = stepsNode.getValue().stream().map(step -> {
				sortStepKeys(nodeAsMap(step));
				return step;
			}).toList();
			setKey(jobNode, STEPS, newSequence(steps));
		});
		sortTuples(jobNode, JOB_KEY_ORDER);
	}

	private static void sortStepKeys(MappingNode stepNode) {
		getKeyAsMap(stepNode, "with").ifPresent(
				with -> sortTuples(
						with,
						Comparator.comparing(GitHubActionsWorkflowFile::keyName)
				)
		);
		sortTuples(stepNode, STEP_KEY_ORDER);
	}

	private static String keyName(NodeTuple tuple) {
		if (tuple.getKeyNode() instanceof ScalarNode keyNode) {
			return keyNode.getValue();
		}
		return tuple.getKeyNode().toString();
	}

	private static void sortTuples(
			MappingNode node,
			List<NodeTuple> tuples,
			Comparator<NodeTuple> order
	) {
		node.setValue(tuples.stream().sorted(order).toList());
	}

	private static void sortTuples(
			MappingNode node,
			Comparator<NodeTuple> order
	) {
		node.setValue(node.getValue().stream().sorted(order).toList());
	}

	public void replaceActionWith(
			String oldAction,
			String newActionRef,
			String newActionVersion
	) {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			var jobNode = nodeAsMap(jobTuple.getValueNode());

			getKeyAsSequence(jobNode, STEPS).ifPresent(stepsNode -> {
				List<Node> steps = stepsNode.getValue().stream().peek(step -> {
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
				}).toList();
				setKey(jobNode, STEPS, newSequence(steps));
			});
		}
	}

}
