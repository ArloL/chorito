package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.RandomCronBuilder;

/**
 * Two chores write {@code .github/workflows/codeql-analysis.yaml}:
 * {@link CodeQlAnalysisChore} regenerates it from the template, and
 * {@link GitHubActionChore}'s {@code updateCodeQlSchedule} then re-reads it and
 * may rewrite the cron. They keep off each other only because the second
 * recognises a schedule the first already randomised.
 * <p>
 * These tests use a real random generator, so a run that rewrote when it should
 * not would land on a different time and fail.
 */
public class CodeQlScheduleOwnershipTest {

	private static final Pattern CRON = Pattern.compile("cron: '([^']*)'");

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void aLegacyWeeklyCronIsConvertedOnce() {
		Path workflow = codeqlWorkflow("26 15 * * 5");

		bothChores();
		String afterFirst = cron(workflow).orElseThrow();

		assertThat(afterFirst).isNotEqualTo("26 15 * * 5");
		assertThat(RandomCronBuilder.isRandomDayOfMonth(afterFirst)).isTrue();

		bothChores();

		assertThat(cron(workflow)).contains(afterFirst);
	}

	@Test
	public void aRandomisedCronSurvivesEveryLaterRun() {
		Path workflow = codeqlWorkflow("14 3 2 * *");

		bothChores();
		bothChores();

		assertThat(cron(workflow)).contains("14 3 2 * *");
	}

	/**
	 * The invariant the two chores rest on, stated where a change to
	 * {@code randomDayOfMonth()} would break it.
	 */
	@Test
	public void whatCodeQlAnalysisChoreWritesIsWhatGitHubActionChoreLeavesAlone() {
		String written = new RandomCronBuilder(context().randomGenerator())
				.randomDayOfMonth();

		assertThat(RandomCronBuilder.isRandomDayOfMonth(written)).isTrue();
	}

	private void bothChores() {
		new CodeQlAnalysisChore().doit(context());
		new GitHubActionChore().updateCodeQlSchedule(context());
	}

	private ChoreContext context() {
		return extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.build();
	}

	private Path codeqlWorkflow(String cron) {
		Path path = extension.root()
				.resolve(".github/workflows/codeql-analysis.yaml");
		FilesSilent.writeString(path, """
				name: CodeQL Analysis

				on:
				  schedule:
				  - cron: '%s'
				permissions: {}

				jobs:
				  analyze:
				    runs-on: ubuntu-latest
				    steps:
				    - uses: actions/checkout@v4
				""".formatted(cron));
		return path;
	}

	private Optional<String> cron(Path workflow) {
		Matcher matcher = CRON.matcher(FilesSilent.readString(workflow));
		return matcher.find() ? Optional.of(matcher.group(1))
				: Optional.empty();
	}

}
