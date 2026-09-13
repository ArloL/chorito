package io.github.arlol.chorito.chores;

import java.util.Map;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.GitHubActionsWorkflowFile;
import io.github.arlol.chorito.tools.Template;

/**
 * Attests the provenance of the files a release publishes, so an installer that
 * checks provenance -- mise with {@code github_attestations} on, for one -- has
 * something to verify them against.
 */
public class AttestReleaseAssetsChore implements Chore {

	private static final String RELEASE_JOB = "release";
	private static final String CREATE_RELEASE_STEP = "Create Release";
	private static final String ATTEST_ACTION = "actions/attest-build-provenance";

	/**
	 * The permissions {@link Template} takes off every workflow it hands out,
	 * granted back here to the jobs that earn them by attesting what they
	 * publish.
	 */
	private static final Map<String, String> ATTEST_PERMISSIONS = Map.of(
			// mints the OIDC token the signing certificate is requested with
			"id-token",
			"write",
			// persists the attestation against the repository
			"attestations",
			"write"
	);

	@Override
	public ChoreContext doit(ChoreContext context) {
		var attestStep = Template.attestReleaseAssetsStep();
		if (attestStep.isEmpty()) {
			return context;
		}

		DirectoryStreams.githubWorkflows(context).forEach(path -> {
			var workflow = new GitHubActionsWorkflowFile(
					FilesSilent.readString(path)
			);
			if (!workflow.hasJob(RELEASE_JOB)
					|| !workflow.releasePublishesAssets()) {
				return;
			}

			String before = workflow.asStringWithoutVersions();
			if (!workflow.hasStepUsing(RELEASE_JOB, ATTEST_ACTION)) {
				workflow.insertStepBefore(
						RELEASE_JOB,
						CREATE_RELEASE_STEP,
						attestStep.orElseThrow()
				);
			}
			workflow.grantJobPermissions(RELEASE_JOB, ATTEST_PERMISSIONS);

			if (!workflow.asStringWithoutVersions().equals(before)) {
				FilesSilent.writeString(path, workflow.asString());
			}
		});

		return context;
	}

}
