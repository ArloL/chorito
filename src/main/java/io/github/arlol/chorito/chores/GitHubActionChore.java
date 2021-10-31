package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.MyFiles;
import io.github.arlol.chorito.tools.Renamer;

public class GitHubActionChore {

	private ChoreContext context;

	public GitHubActionChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() throws Exception {
		// on: workflow_dispatch
		Path workflowsLocation = Paths.get(".github/workflows");
		context.files().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yml");
			}
			return false;
		})
				.map(context::resolve)
				.forEach(
						path -> Renamer.replaceInFilename(path, ".yml", ".yaml")
				);
		MyFiles.writeString(
				context.resolve(".github/workflows/chores.yaml"),
				CHORES_YAML
		);
	}

	private static final String CHORES_YAML = """
			name: Chores

			on:
			  workflow_dispatch:
			  schedule:
			  - cron: '26 15 * * 5'

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
			  chores:
			    name: Chores
			    runs-on: ubuntu-latest
			    steps:
			    - name: Checkout repository
			      uses: actions/checkout@v2.3.5
			    - name: Chores
			      run: |
			        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/ArloL/chorito/HEAD/run-latest.sh)"
			    - name: Create Pull Request
			      uses: peter-evans/create-pull-request@v3.10.1
			""";

}
