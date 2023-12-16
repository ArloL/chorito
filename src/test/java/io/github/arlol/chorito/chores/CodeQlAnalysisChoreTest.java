package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FakeRandomGenerator;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class CodeQlAnalysisChoreTest {

	private static final String EXPECTED_JAVA = """
			name: CodeQL Analysis

			on:
			  push:
			    branches:
			    - main
			  pull_request:
			    branches:
			    - main
			  schedule:
			  - cron: '1 3 1 * *'
			env:
			  JAVA_VERSION: 17

			jobs:
			  debug:
			    runs-on: ubuntu-latest
			    steps:
			    - name: Dump GitHub context
			      env:
			        GITHUB_CONTEXT: ${{ toJSON(github) }}
			      run: echo "$GITHUB_CONTEXT"
			    - name: Dump job context
			      env:
			        JOB_CONTEXT: ${{ toJSON(job) }}
			      run: echo "$JOB_CONTEXT"
			    - name: Dump steps context
			      env:
			        STEPS_CONTEXT: ${{ toJSON(steps) }}
			      run: echo "$STEPS_CONTEXT"
			    - name: Dump runner context
			      env:
			        RUNNER_CONTEXT: ${{ toJSON(runner) }}
			      run: echo "$RUNNER_CONTEXT"
			    - name: Dump strategy context
			      env:
			        STRATEGY_CONTEXT: ${{ toJSON(strategy) }}
			      run: echo "$STRATEGY_CONTEXT"
			    - name: Dump matrix context
			      env:
			        MATRIX_CONTEXT: ${{ toJSON(matrix) }}
			      run: echo "$MATRIX_CONTEXT"
			    - name: Dump environment variables
			      run: set
			  analyze:
			    name: Analyze
			    runs-on: ubuntu-latest
			    steps:
			    - name: Checkout repository
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new CodeQlAnalysisChore().doit(extension.choreContext());
	}

	@Test
	public void testJava() throws Exception {
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new CodeQlAnalysisChore().doit(context);

		Path workflow = context
				.resolve(".github/workflows/codeql-analysis.yaml");
		assertTrue(FilesSilent.exists(workflow));
		assertThat(FilesSilent.readString(workflow)).startsWith(EXPECTED_JAVA);
	}

}
