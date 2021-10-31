package io.github.arlol.chorito.chores;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;

public class EditorConfigChore {

	private static String DEFAULT_EDITORCONFIG = """
			# https://editorconfig.org

			root = true

			[*]
			end_of_line = lf
			insert_final_newline = true

			[*.{bat,cmd,ps1}]
			end_of_line = crlf

			[*.{sh}]
			end_of_line = lf
			""";

	private ChoreContext context;

	public EditorConfigChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() throws IOException {
		Path path = context.resolve(".editorconfig");
		if (!Files.exists(path)) {
			Files.writeString(path, DEFAULT_EDITORCONFIG);
		}
	}

}
