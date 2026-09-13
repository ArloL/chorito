package io.github.arlol.chorito.tools;

import java.util.List;
import java.util.Map;

/**
 * What a chorito-managed workflow is made of: the jobs it names, and the
 * permissions each one needs.
 * <p>
 * This is domain vocabulary shared by every chore that writes a workflow, and
 * it used to be spread as string literals across them -- {@code "release"} in
 * two files, {@code "version"} in two more. A rename that missed one produced a
 * workflow silently only half updated: no error, just an absent change.
 */
public abstract class WorkflowJobs {

	public static final String VERSION = "version";
	public static final String RELEASE = "release";
	public static final String DEPLOY = "deploy";
	public static final String DEBUG = "debug";
	public static final String ANALYZE = "analyze";

	/** The jobs that build a native image, one per operating system. */
	public static final List<String> PLATFORMS = List
			.of("macos", "linux", "windows");

	/**
	 * What each job of a main workflow cannot do its work without.
	 * <p>
	 * Every managed repository gets these. The partition against
	 * {@link #ATTESTATION_PERMISSIONS} is the point of splitting them: both
	 * maps are granted to the same {@link #RELEASE} job of the same file by
	 * different chores, and which one owns a given permission used to be
	 * recorded only in a Javadoc comment on one of the two.
	 */
	public static final Map<String, Map<String, String>> REQUIRED_PERMISSIONS = Map
			.of(
					VERSION,
					Map.of("contents", "write"),
					RELEASE,
					Map.of("contents", "write"),
					DEPLOY,
					Map.of("packages", "write")
			);

	/**
	 * What a release job needs on top, and only once it attests what it
	 * publishes.
	 * <p>
	 * Separate from {@link #REQUIRED_PERMISSIONS} because these are earned, not
	 * given: chorito attests its own releases, so its release job holds them,
	 * and {@link Template} takes them off every workflow it hands out so they
	 * cannot travel to a release job that publishes nothing.
	 * {@code AttestReleaseAssetsChore} grants them back where the attestation
	 * step goes in.
	 */
	public static final Map<String, String> ATTESTATION_PERMISSIONS = Map.of(
			// mints the OIDC token the signing certificate is requested with
			"id-token",
			"write",
			// persists the attestation against the repository
			"attestations",
			"write"
	);

	private WorkflowJobs() {
	}

}
