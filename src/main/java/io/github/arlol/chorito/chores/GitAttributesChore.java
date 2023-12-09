package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitAttributesChore implements Chore {

	private static String DEFAULT_GITATTRIBUTES = """
			*        text=auto eol=lf
			*.bat    text      eol=crlf
			*.cmd    text      eol=crlf
			*.ps1    text      eol=crlf
			*.sh     text      eol=lf
			""";

	@Override
	public void doit(ChoreContext context) {
		Path path = context.resolve(".gitattributes");
		if (!FilesSilent.exists(path)) {
			FilesSilent.writeString(path, DEFAULT_GITATTRIBUTES);
		}
	}

}
