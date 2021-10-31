package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.Newliner;

public class NewlineChore {

	private final ChoreContext context;

	public NewlineChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		context.files()
				.stream()
				.filter(NewlineChore::filterByFilename)
				.map(context::resolve)
				.forEach(Newliner::ensureSystemNewlineAtEof);
	}

	public static boolean filterByFilename(Path file) {
		for (Path part : file) {
			if (".idea".equals(part.toString())) {
				return false;
			}
		}
		return true;
	}

}
