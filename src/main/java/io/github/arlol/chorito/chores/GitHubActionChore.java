package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.nio.file.Paths;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.Renamer;

public class GitHubActionChore {

	private final ChoreContext context;

	public GitHubActionChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
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
		FilesSilent.writeString(
				context.resolve(".github/workflows/chores.yaml"),
				ClassPathFiles.readString("/workflows/chores.yaml")
		);
	}

}
