package io.github.arlol.chorito.chores;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;

public class GitIgnoreChore {

	private final ChoreContext context;

	public GitIgnoreChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() throws Exception {
		Path gitignore = context.resolve(".gitignore");
		if (Files.exists(gitignore)) {
			List<String> lines = new ArrayList<>(Files.readAllLines(gitignore));
			if (!lines.contains(".project")) {
				lines.add(".project");
				Files.write(gitignore, lines);
			}
		}
	}

}
