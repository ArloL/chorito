package io.github.arlol.chorito.chores;

import java.nio.file.Files;
import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;

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

	public void doit() throws Exception {
		Path path = context.resolve(".gitattributes");
		if (!Files.exists(path)) {
			Files.writeString(path, DEFAULT_GITATTRIBUTES);
		}
	}

}
