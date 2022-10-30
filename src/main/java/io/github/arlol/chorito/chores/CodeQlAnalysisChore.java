package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.RandomCronBuilder;

public class CodeQlAnalysisChore {

	private static String JAVA_CODEQL_ANALYSIS = """
			name: CodeQL Analysis

			on:
			  push:
			    branches: main
			  pull_request:
			    branches: main
			  schedule:
			  - cron: ''
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
			  analyze:
			    name: Analyze
			    runs-on: ubuntu-latest
			    steps:
			    - name: Checkout repository
			      uses: actions/checkout@v3.1.0
			    - uses: actions/setup-java@v3.6.0
			      with:
			        java-version: ${{ env.JAVA_VERSION }}
			        distribution: adopt
			        cache: 'maven'
			    - name: Initialize CodeQL
			      uses: github/codeql-action/init@v2.1.29
			      with:
			        languages: java
			    - name: Autobuild
			      uses: github/codeql-action/autobuild@v2.1.29
			    - name: Perform CodeQL Analysis
			      uses: github/codeql-action/analyze@v2.1.29
			    - name: Make sure build did not change anything
			      run: git diff --exit-code
			""";

	private final ChoreContext context;
	private final RandomCronBuilder randomCronBuilder;

	public CodeQlAnalysisChore(ChoreContext context) {
		this.context = context;
		this.randomCronBuilder = new RandomCronBuilder(
				context.randomGenerator()
		);
	}

	public void doit() {
		if (context.hasGitHubRemote()) {
			Path codeQlAnalysis = context
					.resolve(".github/workflows/codeql-analysis.yaml");
			if (!FilesSilent.exists(codeQlAnalysis)) {
				if (FilesSilent.exists(context.resolve("pom.xml"))) {
					FilesSilent.writeString(
							codeQlAnalysis,
							JAVA_CODEQL_ANALYSIS.replace(
									"cron: ''",
									"cron: '" + randomCronBuilder
											.randomDayOfMonth() + "'"
							)
					);
				}
			}
		}
	}

}
