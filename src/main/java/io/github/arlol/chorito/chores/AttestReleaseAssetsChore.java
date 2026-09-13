package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.GitHubActionsWorkflowFile;
import io.github.arlol.chorito.tools.Template;
import io.github.arlol.chorito.tools.WorkflowJobs;

/**
 * Attests the provenance of the files a release publishes, so an installer that
 * checks provenance -- mise with {@code github_attestations} on, for one -- has
 * something to verify them against.
 */
public class AttestReleaseAssetsChore implements Chore {

	private static final String CREATE_RELEASE_STEP = "Create Release";
	private static final String ATTEST_ACTION = "actions/attest-build-provenance";

	@Override
	public ChoreContext doit(ChoreContext context) {
		var attestStep = Template.attestReleaseAssetsStep();
		if (attestStep.isEmpty()) {
			return context;
		}

		GitHubActionsWorkflowFile.updateEach(context, workflow -> {
			if (!workflow.hasJob(WorkflowJobs.RELEASE)
					|| !workflow.releasePublishesAssets()) {
				return;
			}
			if (!workflow.hasStepUsing(WorkflowJobs.RELEASE, ATTEST_ACTION)) {
				workflow.insertStepBefore(
						WorkflowJobs.RELEASE,
						CREATE_RELEASE_STEP,
						attestStep.orElseThrow()
				);
			}
			workflow.grantJobPermissions(
					WorkflowJobs.RELEASE,
					WorkflowJobs.ATTESTATION_PERMISSIONS
			);
		});

		return context;
	}

}
