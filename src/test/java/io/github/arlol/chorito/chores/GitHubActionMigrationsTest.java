package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.chores.GitHubActionChore.Migration;
import io.github.arlol.chorito.chores.GitHubActionChore.Ordering;
import io.github.arlol.chorito.tools.ChoreContext;
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

	@Test
	public void updateMainTriggersFiltersPushAndPullRequestToTheTrunk() {
		Path workflow = workflow("""
				on:
				  push:
				  pull_request:
				    branches:
				    - main
				    types:
				    - reopened
				  schedule:
				  - cron: "14 3 2 * *"
				jobs:
				  build:
				    steps:
				    - run: true
				""");

		chore.updateMainTriggers(context("main"));

		assertThat(workflow).content().contains("""
				on:
				  push:
				    branches:
				    - main
				  pull_request:
				    branches:
				    - main
				  schedule:
				  - cron: "14 3 2 * *"
				""");
		assertThat(workflow).content().doesNotContain("reopened");
	}

	@Test
	public void updateMainTriggersNamesMasterWhenThatIsTheTrunk() {
		Path workflow = workflow("""
				on:
				  push:
				jobs:
				  build:
				    steps:
				    - run: true
				""");

		chore.updateMainTriggers(context("master"));

		assertThat(workflow).content().contains("""
				on:
				  push:
				    branches:
				    - master
				  pull_request:
				    branches:
				    - master
				""");
	}

	/**
	 * A push already naming branches is somebody's decision. Rebuilding the
	 * block would throw away whatever they filtered on without saying so.
	 */
	@Test
	public void updateMainTriggersLeavesAFilteredPushAlone() {
		Path workflow = workflow("""
				on:
				  push:
				    branches:
				    - release
				jobs:
				  build:
				    steps:
				    - run: true
				""");

		chore.updateMainTriggers(context("main"));

		assertThat(workflow).content()
				.contains("- release")
				.doesNotContain("pull_request");
	}

	/**
	 * No branch, no guess. Writing main into a repository still on master would
	 * leave it with triggers that match nothing at all.
	 */
	@Test
	public void updateMainTriggersSkipsWhenTheTrunkIsUnknown() {
		Path workflow = workflow("""
				on:
				  push:
				jobs:
				  build:
				    steps:
				    - run: true
				""");

		chore.updateMainTriggers(extension.choreContext());

		assertThat(workflow).content().doesNotContain("branches");
	}

	@Test
	public void narrowBranchConditionsCollapsesTheDisjunction() {
		Path workflow = workflow(
				releaseGuardedBy(
						"github.ref == 'refs/heads/master'"
								+ " || github.ref == 'refs/heads/main'"
				)
		);

		chore.narrowBranchConditions(context("main"));

		assertThat(workflow).content()
				.contains("if: ${{ github.ref == 'refs/heads/main' }}")
				.doesNotContain("master");
	}

	/**
	 * {@code updateVersionSteps} copies the version job out of chorito's own
	 * main.yaml, which names chorito's branch. A repository on master gets that
	 * copy repointed here -- without it the job could never tag.
	 */
	@Test
	public void narrowBranchConditionsRepointsAMainOnlyGuardAtMaster() {
		Path workflow = workflow(
				releaseGuardedBy("github.ref == 'refs/heads/main'")
		);

		chore.narrowBranchConditions(context("master"));

		assertThat(workflow).content()
				.contains("if: ${{ github.ref == 'refs/heads/master' }}");
	}

	/**
	 * A repository keeping both branches alive still means something by a
	 * condition naming master, so only a name with no branch behind it is
	 * repointed.
	 */
	@Test
	public void narrowBranchConditionsKeepsAGuardWhoseBranchExists() {
		Path workflow = workflow(
				releaseGuardedBy("github.ref == 'refs/heads/master'")
		);

		chore.narrowBranchConditions(context("main", "master"));

		assertThat(workflow).content()
				.contains("if: ${{ github.ref == 'refs/heads/master' }}");
	}

	@Test
	public void removeDeadDependabotConditionDropsItOncePushIsFiltered() {
		Path workflow = workflow(deployExcludingDependabot("""
				on:
				  push:
				    branches:
				    - main
				"""));

		chore.removeDeadDependabotCondition(context("main"));

		assertThat(workflow).content()
				.contains("if: ${{ github.event_name == 'push' }}")
				.doesNotContain("dependabot");
	}

	/**
	 * While push fires for every branch the exclusion is still doing work, so
	 * it stays until the triggers are narrowed.
	 */
	@Test
	public void removeDeadDependabotConditionKeepsItWhilePushIsBare() {
		Path workflow = workflow(deployExcludingDependabot("""
				on:
				  push:
				"""));

		chore.removeDeadDependabotCondition(context("main"));

		assertThat(workflow).content().contains("dependabot");
	}

	/**
	 * The copied version job carries a branch condition, so copying it into a
	 * repository chorito cannot identify would hand it a job that may never tag
	 * -- silently, since nothing fails.
	 */
	@Test
	public void updateVersionStepsSkipsWhenTheTrunkIsUnknown() {
		Path workflow = workflow("""
				jobs:
				  version:
				    runs-on: ubuntu-latest
				    steps:
				    - run: echo mine
				""");

		chore.updateVersionSteps(extension.choreContext());
		assertThat(workflow).content()
				.contains("echo mine")
				.doesNotContain("calver-tag-action");

		chore.updateVersionSteps(context("main"));
		assertThat(workflow).content().contains("calver-tag-action");
	}

	private static String deployExcludingDependabot(String on) {
		return on + """
				jobs:
				  deploy:
				    if: ${{ github.event_name == 'push' && EXCLUSION }}
				    steps:
				    - run: true
				""".replace(
				"EXCLUSION",
				"!startsWith(github.ref, 'refs/heads/dependabot/')"
		);
	}

	private static String releaseGuardedBy(String condition) {
		return """
				jobs:
				  release:
				    if: ${{ CONDITION }}
				    steps:
				    - run: true
				""".replace("CONDITION", condition);
	}

	private ChoreContext context(String... branches) {
		return extension.choreContext()
				.toBuilder()
				.branches(List.of(branches))
				.build();
	}

	private Path workflow(String content) {
		Path path = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(path, content);
		return path;
	}

}
