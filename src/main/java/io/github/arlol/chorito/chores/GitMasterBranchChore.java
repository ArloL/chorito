package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;

public class GitMasterBranchChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		// ChoreContext.mainBranch() prefers main over master, so a repository
		// that renamed and left the old branch behind has already stopped
		// being nagged.
		if (context.mainBranch().filter("master"::equals).isPresent()) {
			System.out.println("You should rename master to main branch");
		}
		return context;
	}

}
