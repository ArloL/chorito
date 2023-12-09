package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;

public interface Chore {

	ChoreContext doit(ChoreContext context);

}
