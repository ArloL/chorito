package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.chores.GitHubActionChore.Migration;
import io.github.arlol.chorito.chores.GitHubActionChore.Ordering;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

/**
 * {@code GitHubActionChoreTest} runs the whole chain against a fixture, which
 * is the right shape for the migrations that genuinely interact and the wrong
 * one for asking whether a single migration does its job. These tests run one
 * migration at a time, and check that the order the chain depends on still
 * holds.
 */
public class GitHubActionMigrationsTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private final GitHubActionChore chore = new GitHubActionChore();

	/**
	 * The dependencies are real and correctly co-located -- same class, same
	 * file, same commit -- they were just never written down. Now that they
	 * are, reordering the list fails here instead of silently producing a
	 * migration that stops matching.
	 */
	@Test
	public void everyDeclaredOrderingHolds() {
		List<String> order = chore.migrations()
				.stream()
				.map(Migration::name)
				.toList();

		for (Ordering ordering : GitHubActionChore.ORDERINGS) {
			assertThat(order)
					.as(
							"both ends of %s -> %s exist",
							ordering.first(),
							ordering.second()
					)
					.contains(ordering.first(), ordering.second());
			assertThat(order.indexOf(ordering.first()))
					.as(
							"%s must run before %s: %s",
							ordering.first(),
							ordering.second(),
							ordering.because()
					)
					.isLessThan(order.indexOf(ordering.second()));
		}
	}

	@Test
	public void everyMigrationIsNamedAfterTheMethodItRuns() {
		assertThat(chore.migrations()).extracting(Migration::name)
				.doesNotHaveDuplicates()
				.isNotEmpty();
	}

	@Test
	public void useSpecificActionVersionsPinsABareSetupJava() {
		Path workflow = workflow("""
				jobs:
				  build:
				    steps:
				    - uses: actions/setup-java@v3
				""");

		chore.useSpecificActionVersions(extension.choreContext());

		assertThat(workflow).content().contains("actions/setup-java@v3.5.1");
	}

	/**
	 * The first declared ordering, from the consuming end: the Graal block only
	 * matches once {@code useSpecificActionVersions} has pinned the bare
	 * version it names.
	 */
	@Test
	public void migrateToGraalSetupActionNeedsTheVersionPinnedFirst() {
		Path workflow = workflow("""
				jobs:
				  build:
				    steps:
				    - uses: actions/setup-java@v3
				      with:
				        java-version: ${{ env.JAVA_VERSION }}
				        distribution: adopt
				        cache: 'maven'
				    - name: Setup Graalvm
				      uses: DeLaGuardo/setup-graalvm@5.0
				      with:
				        graalvm: ${{ env.GRAALVM_VERSION }}
				        java: java${{ env.JAVA_VERSION }}
				    - name: Install native-image module
				      run: gu install native-image
				""");

		chore.migrateToGraalSetupAction(extension.choreContext());
		assertThat(workflow).content().doesNotContain("graalvm/setup-graalvm");

		chore.useSpecificActionVersions(extension.choreContext());
		chore.migrateToGraalSetupAction(extension.choreContext());
		assertThat(workflow).content().contains("graalvm/setup-graalvm@v1.0.7");
	}

	/**
	 * The second declared ordering, from the destroying end: rewriting adopt to
	 * temurin removes the string the Graal block matches on.
	 */
	@Test
	public void migratingAdoptToTemurinFirstStopsTheGraalBlockMatching() {
		Path workflow = workflow("""
				jobs:
				  build:
				    steps:
				    - uses: actions/setup-java@v3.5.1
				      with:
				        java-version: ${{ env.JAVA_VERSION }}
				        distribution: adopt
				        cache: 'maven'
				    - name: Setup Graalvm
				      uses: DeLaGuardo/setup-graalvm@5.0
				      with:
				        graalvm: ${{ env.GRAALVM_VERSION }}
				        java: java${{ env.JAVA_VERSION }}
				    - name: Install native-image module
				      run: gu install native-image
				""");

		chore.migrateJavaDistributionFromAdoptToTemurin(
				extension.choreContext()
		);
		chore.migrateToGraalSetupAction(extension.choreContext());

		assertThat(workflow).content().doesNotContain("graalvm/setup-graalvm");
	}

	@Test
	public void quoteRedirectsBracesAndQuotesGithubEnv() {
		Path workflow = workflow("""
				jobs:
				  build:
				    steps:
				    - run: echo "a=b" > $GITHUB_ENV
				""");

		chore.quoteRedirects(extension.choreContext());

		assertThat(workflow).content().contains("> \"${GITHUB_ENV}\"");
	}

	@Test
	public void replaceSetOutputRewritesTheDeprecatedCommand() {
		// The rewrite keys on the echo starting its own line, so only a block
		// run reaches it -- a one line "- run: echo ..." is left alone.
		Path workflow = workflow("""
				jobs:
				  build:
				    steps:
				    - run: |
				        echo "::set-output name=version::1.2.3"
				""");

		chore.replaceSetOutput(extension.choreContext());

		assertThat(workflow).content()
				.contains("echo \"version=1.2.3\" >> \"${GITHUB_OUTPUT}\"");
	}

	@Test
	public void removeNeedsVersionOutputsChangelogDropsTheBodyLine() {
		Path workflow = workflow("""
				jobs:
				  release:
				    steps:
				    - uses: ncipollo/release-action@v1
				      with:
				        body: ${{ needs.version.outputs.changelog }}
				        tag: v1
				""");

		chore.removeNeedsVersionOutputsChangelog(extension.choreContext());

		assertThat(workflow).content()
				.doesNotContain("changelog")
				.contains("tag: v1");
	}

	private Path workflow(String content) {
		Path path = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(path, content);
		return path;
	}

}
