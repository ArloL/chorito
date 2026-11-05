package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.GitHubActionsWorkflowFile;
import io.github.arlol.chorito.tools.JavaVersions;
import io.github.arlol.chorito.tools.Template;
import io.github.arlol.chorito.tools.WorkflowJobs;

/**
 * Builds arm64 Linux in every GraalVM repository, and gives a native CLI's
 * release assets the os and arch tokens an installer picks by.
 * <p>
 * A library that proves it works inside a native image ships no binary, but it
 * is tested on arm like the rest; its job has no upload step, as its other
 * platform jobs have none ({@code GitHubActionChore} removes them, and would
 * strip this one on the next run were it added with them).
 * <p>
 * mise, ubi, aqua and the rest score a release asset by the tokens in its name
 * and then run the file inside under the name that file already has. The assets
 * chorito used to ship named only the os, so {@code mise use github:ArloL/...}
 * would hand a machine an executable for an architecture it had never checked,
 * or a zip of jars with no binary in it at all. Each archive here holds exactly
 * one executable, named after the repository.
 * <p>
 * The {@code Prepare artifacts} step is chorito's to own, the way the platform
 * jobs already are: it is rewritten from the template rather than patched, so a
 * repository that edited it by hand loses that edit. What the step names --
 * {@code ARTIFACT} and the version -- comes from the workflow around it, so one
 * template serves every repository.
 */
public class NativeReleaseChore implements Chore {

	private static final String PREPARE_ARTIFACTS_STEP = "Prepare artifacts";
	private static final String DOWNLOAD_ARTIFACT_ACTION = "actions/download-artifact";

	@Override
	public ChoreContext doit(ChoreContext context) {
		var template = Template.mainWorkflow();
		var armJob = template.getJob(WorkflowJobs.LINUX_ARM);
		var prepareStep = template
				.getStepByName(WorkflowJobs.RELEASE, PREPARE_ARTIFACTS_STEP);
		if (armJob.isEmpty() || prepareStep.isEmpty()) {
			return context;
		}

		boolean buildsOnGraalVm = JavaVersions.buildsOnGraalVm(context);
		GitHubActionsWorkflowFile.updateEach(context, workflow -> {
			if (!buildsOnGraalVm || !workflow.hasJob(WorkflowJobs.LINUX)
					|| workflow.asString().contains("gluonfx")) {
				return;
			}
			workflow.putJobAfter(
					WorkflowJobs.LINUX_ARM,
					armJob,
					WorkflowJobs.LINUX
			);
			if (!workflow.hasStepUsing(
					WorkflowJobs.RELEASE,
					DOWNLOAD_ARTIFACT_ACTION
			)) {
				workflow.removeStepByName(
						WorkflowJobs.LINUX_ARM,
						"Move artifacts"
				);
				workflow.removeStepUsing(
						WorkflowJobs.LINUX_ARM,
						"actions/upload-artifact"
				);
			}
			// Without these the release can start while the arm build is still
			// running, and the step below fails on a binary that is not there
			// yet -- intermittently, depending on which job finished first.
			workflow.addJobNeeds(
					WorkflowJobs.RELEASE,
					WorkflowJobs.LINUX_ARM,
					WorkflowJobs.LINUX
			);
			workflow.addJobNeeds(
					WorkflowJobs.REQUIRED_STATUS_CHECK,
					WorkflowJobs.LINUX_ARM,
					WorkflowJobs.LINUX
			);
			if (buildsNativeCli(workflow)) {
				workflow.replaceStepByName(
						WorkflowJobs.RELEASE,
						PREPARE_ARTIFACTS_STEP,
						prepareStep.orElseThrow()
				);
			}
		});

		return context;
	}

	/**
	 * Whether this workflow releases native binaries (GraalVM is checked by the
	 * caller).
	 * <p>
	 * Both conditions matter. A repository can run GraalVM to prove its library
	 * works inside a native image without ever shipping one, and its release
	 * job then has no binaries to lay out; keying on the download is what tells
	 * the two apart, the same signal {@code GitHubActionChore} uses. The step
	 * being rewritten assembles {@code target/artifacts}, so a release
	 * attaching assets from its own paths -- the shape chorito shipped before
	 * -- is left for the migrations in that chore to bring forward first.
	 */
	private boolean buildsNativeCli(GitHubActionsWorkflowFile workflow) {
		return workflow
				.hasStepUsing(WorkflowJobs.RELEASE, DOWNLOAD_ARTIFACT_ACTION)
				&& workflow.releasePublishesAssets();
	}

}
