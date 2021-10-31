package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.Newliner;

public class ShellScriptNewlineChore {

	private ChoreContext context;

	public ShellScriptNewlineChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		context.files().stream().filter(name -> {
			var filename = name.toString();
			return filename.endsWith(".sh");
		}).map(context::resolve).forEach(Newliner::makeAllNewlinesLf);
	}

}
