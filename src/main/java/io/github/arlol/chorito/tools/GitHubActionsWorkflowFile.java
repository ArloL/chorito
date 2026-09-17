package io.github.arlol.chorito.tools;

import static io.github.arlol.chorito.tools.Yamls.copyValue;
import static io.github.arlol.chorito.tools.Yamls.getKeyAsMap;
import static io.github.arlol.chorito.tools.Yamls.getKeyAsNode;
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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
	private static final String MAIN_BRANCH = "main";
	private static final List<String> ON_BRANCH_TRIGGERS = List
			.of("push", "pull_request");
	private static final String SETUP_JAVA_ACTION = "actions/setup-java";
	private static final String DISTRIBUTION_TEMURIN = "temurin";
	private static final String JAVA_VERSION = "java-version";
	private static final String JAVA_VERSION_ENV = "JAVA_VERSION";
	private static final String ENV = "env";
	private static final String WITH = "with";
	// Adoptium's semver carries a build suffix setup-java cannot resolve.
	private static final String RENOVATE_JAVA_VERSION_COMMENT = "renovate: datasource=java-version depName=java extractVersion=^(?<version>\\d+\\.\\d+\\.\\d+)";
	private static final Pattern USES_VERSION = Pattern
			.compile("(?m)^([ \\t-]*uses:[^@\\n]*)@[^\\n]*");
	/**
	 * The pins Renovate's customManagers:githubActionsVersions preset updates:
	 * a "# renovate:" comment above an env var ending in _VERSION.
	 */
	private static final Pattern TOOL_VERSION = Pattern.compile(
			"(?m)^([ \\t]*# renovate: [^\\n]*\\n[ \\t]*[A-Za-z0-9_]+_VERSION\\s*:)[^\\n]*"
	);

	/**
	 * Runs {@code change} against every workflow of {@code context}, writing
	 * back only the ones it actually changed.
	 * <p>
	 * The chores that rewrite workflows communicate through the files
	 * themselves, each one re-reading what the last wrote. The read, the
	 * compare and the conditional write are the protocol that makes that safe
	 * -- a workflow rewritten with no change still churns the file and the
	 * commit -- and it was spelled out separately in four places. Stated once
	 * here, a chore says what it changes and nothing else.
	 * <p>
	 * Versions are ignored when deciding whether anything changed, so a
	 * workflow whose action or tool pins Renovate has bumped is not rewritten
	 * back.
	 */
	public static void updateEach(
			ChoreContext context,
			Consumer<GitHubActionsWorkflowFile> change
	) {
		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			var workflow = new GitHubActionsWorkflowFile(
					FilesSilent.readString(path)
			);
			String before = workflow.asStringWithoutVersions();
			change.accept(workflow);
			if (!workflow.asStringWithoutVersions().equals(before)) {
				FilesSilent.writeString(path, workflow.asString());
			}
		});
	}

	public static String removeVersions(String input) {
		String withoutUses = USES_VERSION.matcher(input).replaceAll("$1@\n");
		return TOOL_VERSION.matcher(withoutUses).replaceAll("$1");
	}

	public static boolean pinsToolVersions(String input) {
		return TOOL_VERSION.matcher(input).find();
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

	/**
	 * Puts {@code job} under {@code name}, directly after the job called
	 * {@code afterName} when it is not there yet.
	 * <p>
	 * {@link #setJob} writes into a job that already exists and is silent when
	 * it does not, which is what the chores syncing a job from the template
	 * want. Adding a platform to a repository is the other case: the job has
	 * never been there, and appending it to the end of the file would put a
	 * build job below the release that waits on it.
	 */
	public void putJobAfter(
			String name,
			Optional<MappingNode> job,
			String afterName
	) {
		if (job.isEmpty() || getJobs().isEmpty()) {
			return;
		}
		if (hasJob(name)) {
			setJob(name, job);
			return;
		}
		MappingNode jobs = getJobs().orElseThrow();
		List<NodeTuple> tuples = new ArrayList<>(jobs.getValue());
		NodeTuple added = newTuple(name, job.orElseThrow());
		int after = indexOfKey(tuples, afterName);
		if (after < 0) {
			tuples.add(added);
		} else {
			tuples.add(after + 1, added);
		}
		jobs.setValue(tuples);
	}

	/**
	 * Adds {@code needed} to what {@code jobName} waits for, directly after
	 * {@code afterName}, and does nothing when it is already listed.
	 * <p>
	 * Only a {@code needs} written as a sequence is touched. The single-job
	 * form -- {@code needs: version} on every platform job -- is a scalar, and
	 * a job waiting on exactly one other is never the one a platform gets added
	 * to.
	 */
	public void addJobNeeds(String jobName, String needed, String afterName) {
		var needs = getKeyAsSequence(getJob(jobName), "needs");
		if (needs.isEmpty()) {
			return;
		}
		List<Node> values = new ArrayList<>(needs.orElseThrow().getValue());
		if (values.stream()
				.anyMatch(
						value -> scalarValue(Optional.of(value))
								.filter(needed::equals)
								.isPresent()
				)) {
			return;
		}
		int after = indexOfScalar(values, afterName);
		// Plain, so the added entry reads like the ones around it: newScalar
		// defaults to double quotes, and `- "linux-arm"` beside `- linux` is a
		// diff that says nothing.
		Node added = newScalar(needed, ScalarStyle.PLAIN);
		if (after < 0) {
			values.add(added);
		} else {
			values.add(after + 1, added);
		}
		setKey(getJob(jobName).orElseThrow(), "needs", newSequence(values));
	}

	/**
	 * Replaces the step of {@code jobName} called {@code stepName} with
	 * {@code step}, or does nothing when that step is not there.
	 */
	public void replaceStepByName(String jobName, String stepName, Node step) {
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
				steps.set(i, step);
				setKey(job.orElseThrow(), STEPS, newSequence(steps));
				return;
			}
		}
	}

	private static int indexOfKey(List<NodeTuple> tuples, String key) {
		for (int i = 0; i < tuples.size(); i++) {
			if (scalarValue(Optional.of(tuples.get(i).getKeyNode()))
					.filter(key::equals)
					.isPresent()) {
				return i;
			}
		}
		return -1;
	}

	private static int indexOfScalar(List<Node> values, String value) {
		for (int i = 0; i < values.size(); i++) {
			if (scalarValue(Optional.of(values.get(i))).filter(value::equals)
					.isPresent()) {
				return i;
			}
		}
		return -1;
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

	/**
	 * Whether {@code on.push} is present with nothing written under it.
	 * <p>
	 * A bare push is the shape chorito shipped, and the only one safe to
	 * replace wholesale. Anything under push -- branches, tags, paths -- is
	 * somebody's decision, and rebuilding the block would discard it without
	 * saying so.
	 */
	public boolean hasBarePush() {
		return getOn().flatMap(on -> getKeyAsNode(on, "push"))
				.filter(push -> !(push instanceof MappingNode))
				.isPresent();
	}

	/**
	 * Whether {@code on.push} names the branches it runs for.
	 */
	public boolean hasOnPushBranches() {
		return getOn().flatMap(on -> getKeyAsMap(on, "push"))
				.flatMap(push -> getKeyAsSequence(push, "branches"))
				.isPresent();
	}

	/**
	 * Replaces the push and pull_request triggers with ones filtered to
	 * {@code branch}, keeping every other trigger and the order they are
	 * written in.
	 * <p>
	 * The two are rebuilt rather than edited in place because a workflow
	 * reaching this may have only one of them, or neither in the position it
	 * ends up in. {@link #sortKeys()} orders the top level of a workflow and
	 * the insides of a job, but nothing orders the triggers, so writing push
	 * and pull_request first here is what keeps the block readable.
	 */
	public void setOnPushAndPullRequestBranches(String branch) {
		getOn().ifPresent(on -> {
			List<NodeTuple> tuples = new ArrayList<>();
			tuples.add(newTuple("push", branchFilter(branch)));
			tuples.add(newTuple("pull_request", branchFilter(branch)));
			on.getValue()
					.stream()
					.filter(
							tuple -> !ON_BRANCH_TRIGGERS
									.contains(keyName(tuple))
					)
					.forEach(tuples::add);
			on.setValue(tuples);
		});
	}

	/**
	 * Points existing push and pull_request branch filters at {@code branch}.
	 * <p>
	 * chorito's own workflows are the templates it ships, and they say
	 * {@code main} because that is the branch chorito uses. A repository still
	 * on {@code master} needs the same file with the name swapped: a filter
	 * naming a branch that does not exist matches nothing, and the workflow
	 * then never runs at all -- no failed run to notice, just silence.
	 */
	public void renameOnBranches(String branch) {
		if (MAIN_BRANCH.equals(branch)) {
			return;
		}
		getOn().ifPresent(on -> ON_BRANCH_TRIGGERS.forEach(trigger -> {
			getKeyAsMap(on, trigger).ifPresent(triggerNode -> {
				getKeyAsSequence(triggerNode, "branches").ifPresent(
						branches -> setKey(
								triggerNode,
								"branches",
								newSequence(
										branches.getValue()
												.stream()
												.map(
														node -> renamedBranch(
																node,
																branch
														)
												)
												.toList()
								)
						)
				);
			});
		}));
	}

	private static Node renamedBranch(Node node, String branch) {
		if (node instanceof ScalarNode scalar
				&& MAIN_BRANCH.equals(scalar.getValue())) {
			return newScalar(branch, ScalarStyle.PLAIN);
		}
		return node;
	}

	private static MappingNode branchFilter(String branch) {
		// A fresh node per call: two tuples sharing one node make the emitter
		// write an anchor and an alias instead of the block twice.
		return newMap(
				newTuple(
						"branches",
						newSequence(newScalar(branch, ScalarStyle.PLAIN))
				)
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
	 * Renovate made survives. A version written straight into
	 * {@code java-version} is how chorito pinned before the env var, and is
	 * read so that pin moves across too.
	 */
	public Optional<String> getPinnedJavaVersion() {
		List<String> versions = new ArrayList<>();
		forEachTemurinSetupJava(step -> {
			var version = scalarValue(
					getKeyAsNode(getKeyAsMap(step, ENV), JAVA_VERSION_ENV)
			).or(
					() -> scalarValue(
							getKeyAsNode(getKeyAsMap(step, WITH), JAVA_VERSION)
					).filter(v -> !v.startsWith("${{"))
			);
			version.ifPresent(versions::add);
		});
		return versions.stream().findFirst();
	}

	/**
	 * Pins every Temurin setup-java step to {@code version}, replacing whatever
	 * the step said before. Carrying a version across belongs to the caller,
	 * which reads it with {@link #getPinnedJavaVersion()} first: a chore that
	 * rebuilds a workflow from a template must not inherit the template's own
	 * pin, and chorito's template for this file is a symlink to the workflow it
	 * runs itself.
	 * <p>
	 * The version lives in a step env var so Renovate's
	 * customManagers:githubActionsVersions preset finds it.
	 */
	public void pinTemurinJavaVersion(String version) {
		forEachTemurinSetupJava(step -> {
			var env = getKeyAsMap(step, ENV).orElseGet(() -> {
				var newEnv = newMap();
				var tuples = new ArrayList<NodeTuple>();
				for (NodeTuple tuple : step.getValue()) {
					if (scalarValue(tuple.getKeyNode()).filter(WITH::equals)
							.isPresent()) {
						tuples.add(newTuple(ENV, newEnv));
					}
					tuples.add(tuple);
				}
				step.setValue(tuples);
				return newEnv;
			});
			removeKey(Optional.of(env), JAVA_VERSION_ENV);
			var keyNode = newScalar(JAVA_VERSION_ENV, ScalarStyle.PLAIN);
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
			var envTuples = new ArrayList<>(env.getValue());
			envTuples.add(
					new NodeTuple(
							keyNode,
							newScalar(version, ScalarStyle.PLAIN)
					)
			);
			env.setValue(envTuples);

			var with = getKeyAsMap(step, WITH);
			removeKey(with, "java-version-file");
			// Removed rather than overwritten: the key of an older pin carries
			// the renovate comment that now belongs to the env var.
			removeKey(with, JAVA_VERSION);
			setKey(
					with.orElseThrow(),
					JAVA_VERSION,
					newScalar(
							"${{ env." + JAVA_VERSION_ENV + " }}",
							ScalarStyle.PLAIN
					)
			);
		});
	}

	/**
	 * Runs {@code body} against every setup-java step asking for Temurin. Those
	 * are the steps that build no native image, so they are the ones whose JDK
	 * is chorito's to decide.
	 */
	private void forEachTemurinSetupJava(Consumer<MappingNode> body) {
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
				if (scalarValue(
						getKeyAsNode(
								getKeyAsMap(stepNode, WITH),
								"distribution"
						)
				).filter(DISTRIBUTION_TEMURIN::equals).isEmpty()) {
					continue;
				}
				body.accept(stepNode);
			}
		}
	}

	/**
	 * Points every Temurin setup-java step back at {@code .tool-versions},
	 * dropping a pin and the renovate comment above it.
	 */
	public void useToolVersionsFile() {
		forEachTemurinSetupJava(step -> {
			var env = getKeyAsMap(step, ENV);
			removeKey(env, JAVA_VERSION_ENV);
			if (env.map(e -> e.getValue().isEmpty()).orElse(false)) {
				removeKey(Optional.of(step), ENV);
			}
			var with = getKeyAsMap(step, WITH);
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

	/**
	 * Takes {@code permissions} away from every job that has them.
	 * <p>
	 * The counterpart to {@link #grantJobPermissions(String, Map)}, and the one
	 * thing a template needs that granting cannot do: chorito's own workflows
	 * grant themselves permissions no other repository has earned, and the
	 * templates are those workflows.
	 *
	 * @see Template
	 */
	public void revokeJobPermissions(Set<String> permissions) {
		for (NodeTuple jobTuple : getJobs().map(MappingNode::getValue)
				.orElse(List.of())) {
			var job = nodeAsMap(jobTuple.getValueNode());
			var granted = getKeyAsMap(job, PERMISSIONS);
			if (granted.isEmpty()) {
				continue;
			}
			var kept = granted.orElseThrow()
					.getValue()
					.stream()
					.filter(
							t -> scalarValue(t.getKeyNode())
									.filter(permissions::contains)
									.isEmpty()
					)
					.toList();
			if (kept.isEmpty()) {
				removeKey(Optional.of(job), PERMISSIONS);
			} else {
				granted.orElseThrow().setValue(new ArrayList<>(kept));
			}
		}
	}

	private List<Node> steps(String jobName) {
		return getKeyAsSequence(getJob(jobName), STEPS)
				.map(SequenceNode::getValue)
				.orElse(List.of());
	}

	/**
	 * Whether the release job hands the files under {@code target/artifacts} to
	 * whatever publishes the release. That is the path the attestation names as
	 * its subject, so a release publishing from anywhere else -- the older jobs
	 * attach assets one by one from their own paths -- answers false.
	 */
	public boolean releasePublishesAssets() {
		return jobMentions(WorkflowJobs.RELEASE, "target/artifacts/");
	}

	public void removeStepByName(String jobName, String stepName) {
		removeSteps(
				jobName,
				step -> scalarValue(getKeyAsNode(nodeAsMap(step), "name"))
						.filter(stepName::equals)
						.isPresent()
		);
	}

	public void removeStepUsing(String jobName, String actionName) {
		removeSteps(
				jobName,
				step -> scalarValue(getKeyAsNode(nodeAsMap(step), "uses"))
						.filter(uses -> uses.startsWith(actionName + "@"))
						.isPresent()
		);
	}

	private void removeSteps(String jobName, Predicate<Node> unwanted) {
		var job = getJob(jobName);
		var stepsNode = getKeyAsSequence(job, STEPS);
		if (stepsNode.isEmpty()) {
			return;
		}
		List<Node> kept = stepsNode.orElseThrow()
				.getValue()
				.stream()
				.filter(unwanted.negate())
				.toList();
		setKey(job.orElseThrow(), STEPS, newSequence(kept));
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

}
