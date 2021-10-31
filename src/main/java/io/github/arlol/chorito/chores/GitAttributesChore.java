package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitAttributesChore {

	private static String DEFAULT_GITATTRIBUTES = """
			*        text=auto eol=lf
			*.bat    text      eol=crlf
			*.cmd    text      eol=crlf
			*.ps1    text      eol=crlf
			*.sh     text      eol=lf
			""";

	private final ChoreContext context;

	public GitAttributesChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path path = context.resolve(".gitattributes");
		if (!FilesSilent.exists(path)) {
			FilesSilent.writeString(path, DEFAULT_GITATTRIBUTES);
		}
	}

}
