package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

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

	public void doit() {
		Path path = context.resolve(".editorconfig");
		if (!FilesSilent.exists(path)) {
			FilesSilent.writeString(path, DEFAULT_EDITORCONFIG);
		}
	}

}
