package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitIgnoreChore {

	private final ChoreContext context;

	public GitIgnoreChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path gitignore = context.resolve(".gitignore");
		if (FilesSilent.exists(gitignore)) {
			List<String> lines = new ArrayList<>(
					FilesSilent.readAllLines(gitignore)
			);
			if (!lines.contains(".project")) {
				lines.add(".project");
				FilesSilent.write(gitignore, lines);
			}
		} else {
			FilesSilent.writeString(gitignore, ".project\n");
		}
	}

}
