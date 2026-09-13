package io.github.arlol.chorito.tools;

import java.util.Optional;
import java.util.Set;

import org.snakeyaml.engine.v2.nodes.Node;

/**
 * The workflows chorito ships to the repositories it manages.
 * <p>
 * {@code src/main/resources/io/github/arlol/chorito/github-settings} is a
 * symlink to chorito's own {@code .github}, so one set of files serves two
 * requirement sets: what chorito needs to build and release itself, and what an
 * arbitrary repository should be handed as a default. Keeping the symlink is
 * deliberate -- it guarantees every template is a workflow that actually runs
 * -- but it means an edit to chorito's own CI is an edit to the product.
 * <p>
 * This class is the one place that reads those resources, and
 * {@link #CHORITO_ONLY_PERMISSIONS} plus {@link #generalise} are the one place
 * that says which parts of them are chorito's alone. Before it, that knowledge
 * lived as prose in two Javadoc comments beside two hand-written corrections in
 * two different chores, and each one was written after someone noticed a leak
 * that had already shipped. Read a template through here and what comes back is
 * already general; add to the list below when chorito grants itself something
 * new.
 */
public abstract class Template {

	private static final String WORKFLOWS = "github-settings/workflows/";
	private static final String ATTEST_STEP = "Attest the release assets";

	/**
	 * Permissions chorito's own jobs hold that no repository gets by default.
	 * chorito attests its releases, so its release job mints an OIDC token and
	 * writes attestations; copying that across would hand those to every
	 * release job whether or not it publishes anything.
	 * <p>
	 * Taken from {@link WorkflowJobs#ATTESTATION_PERMISSIONS} rather than
	 * listed again, so what is stripped here and what
	 * {@code AttestReleaseAssetsChore} grants back cannot drift apart. That
	 * chore reads the step earning them through
	 * {@link #attestReleaseAssetsStep()}.
	 */
	private static final Set<String> CHORITO_ONLY_PERMISSIONS = WorkflowJobs.ATTESTATION_PERMISSIONS
			.keySet();

	private Template() {
	}

	public static GitHubActionsWorkflowFile mainWorkflow() {
		return generalise(load("main.yaml"));
	}

	public static GitHubActionsWorkflowFile choresWorkflow() {
		return generalise(load("chores.yaml"));
	}

	public static GitHubActionsWorkflowFile checkActionsWorkflow() {
		return generalise(load("check-actions.yaml"));
	}

	public static GitHubActionsWorkflowFile codeQlAnalysisWorkflow() {
		return generalise(load("codeql-analysis.yaml"));
	}

	/**
	 * The step chorito's own release job uses to attest its assets.
	 * <p>
	 * The one deliberate read of a chorito-specific element: the step is what
	 * {@code AttestReleaseAssetsChore} copies into a release job that publishes
	 * assets, together with the permissions {@link #generalise} takes off every
	 * other template. Empty when chorito stops attesting its own releases, at
	 * which point that chore has nothing to offer and steps aside.
	 */
	public static Optional<Node> attestReleaseAssetsStep() {
		return load("main.yaml")
				.getStepByName(WorkflowJobs.RELEASE, ATTEST_STEP);
	}

	private static GitHubActionsWorkflowFile load(String workflow) {
		return new GitHubActionsWorkflowFile(
				ClassPathFiles.readString(WORKFLOWS + workflow)
		);
	}

	/**
	 * Takes off what belongs to chorito and leaves what belongs to everyone.
	 * <p>
	 * Add to this when chorito grants itself something new, and to
	 * {@code TemplateTest} so the next omission fails the build rather than
	 * shipping.
	 */
	private static GitHubActionsWorkflowFile generalise(
			GitHubActionsWorkflowFile workflow
	) {
		workflow.revokeJobPermissions(CHORITO_ONLY_PERMISSIONS);
		// chorito builds a native image and so pins GraalVM in .tool-versions.
		// Its jobs that build no native image -- analysis, publishing -- cannot
		// use that, so they pin a Temurin of their own. That pin is chorito's
		// answer to chorito's problem, not a default: pointing back at
		// .tool-versions is the general form, and a chore that knows the target
		// repository builds on GraalVM pins again on top, as
		// CodeQlAnalysisChore does. The GraalVM setup-java steps are left
		// alone; this only touches the Temurin ones.
		workflow.useToolVersionsFile();
		return workflow;
	}

}
