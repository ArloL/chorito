package io.github.arlol.chorito.commands;

import io.github.arlol.chorito.chores.DependabotChore;
import io.github.arlol.chorito.chores.Ec4jChore;
import io.github.arlol.chorito.chores.EditorConfigChore;
import io.github.arlol.chorito.chores.GitAttributesChore;
import io.github.arlol.chorito.chores.GitHubActionChore;
import io.github.arlol.chorito.chores.GitIgnoreChore;
import io.github.arlol.chorito.chores.MavenWrapperChore;
import io.github.arlol.chorito.chores.ShellScriptNewlineChore;
import io.github.arlol.chorito.chores.WindowsScriptNewlineChore;
import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.GitChoreContext;

public class ChoritoCommand {

	private final ChoreContext context;

	public ChoritoCommand(ChoreContext context) {
		this.context = context;
	}

	public ChoritoCommand(String root) {
		this.context = new GitChoreContext(root);
	}

	public void execute() throws Exception {
		new GitAttributesChore(context).doit();
		new EditorConfigChore(context).doit();
		new MavenWrapperChore(context).doit();
		new WindowsScriptNewlineChore(context).doit();
		new ShellScriptNewlineChore(context).doit();
		new GitHubActionChore(context).doit();
		new DependabotChore(context).doit();
		new GitIgnoreChore(context).doit();
		new Ec4jChore(context).doit();
	}

}
