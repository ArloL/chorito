package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.Newliner;

public class WindowsScriptNewlineChore {

	private ChoreContext context;

	public WindowsScriptNewlineChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		context.files().stream().filter(name -> {
			var filename = name.toString();
			return filename.endsWith(".cmd") || filename.endsWith(".bat")
					|| filename.endsWith(".ps1");
		}).map(context::resolve).forEach(Newliner::makeAllNewlinesCrLf);
	}

}
