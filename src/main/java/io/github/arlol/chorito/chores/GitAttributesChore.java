package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ExistingFileUpdater;

public class GitAttributesChore implements Chore {

	private static final String GITATTRIBUTES = """
			# See https://git-scm.com/docs/gitattributes for more about gitattributes files.

			*        text=auto eol=lf
			*.bat    text      eol=crlf
			*.cmd    text      eol=crlf
			*.ps1    text      eol=crlf
			*.sh     text      eol=lf
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path path = context.resolve(".gitattributes");
		ExistingFileUpdater.update(path, GITATTRIBUTES);
		return context.refresh();
	}

}
