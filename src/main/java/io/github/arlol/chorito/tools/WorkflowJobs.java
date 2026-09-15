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

	public static final String REQUIRED_STATUS_CHECK = "required-status-check";

	/** The job that builds the x64 Linux native image. */
	public static final String LINUX = "linux";

	/**
	 * The job that builds the arm64 Linux native image.
	 * <p>
	 * The one platform job chorito adds to a repository rather than syncing:
	 * the others predate it everywhere it runs.
	 * {@code NativeReleaseLayoutChore} puts it in, after {@link #LINUX}, and
	 * wires the jobs that wait on it.
	 */
	public static final String LINUX_ARM = "linux-arm";

	/**
	 * The jobs that build a native image, one per operating system and
	 * architecture.
	 * <p>
	 * GraalVM cannot cross-compile, so this is one job per runner rather than
	 * one per target: macOS is arm64 alone because GitHub no longer offers an
	 * Intel macOS runner, and Windows is x64 alone because it offers no arm
	 * one.
	 */
	public static final List<String> PLATFORMS = List
			.of("macos", LINUX, LINUX_ARM, "windows");

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
