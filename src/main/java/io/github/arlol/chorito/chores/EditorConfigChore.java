package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;

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

			[*.sh]
			end_of_line = lf
			""";

	private final ChoreContext context;

	public EditorConfigChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path path = context.resolve(".editorconfig");
		if (!FilesSilent.exists(path)) {
			FilesSilent.writeString(path, DEFAULT_EDITORCONFIG);
		}
		var content = FilesSilent.readAllLines(path);
		content = removeBracketsFromSingleExtensionGroups(content);
		FilesSilent.write(path, content);
	}

	public static List<String> removeBracketsFromSingleExtensionGroups(
			List<String> content
	) {
		// make sure single file name matches are without brackets
		return content.stream().map(string -> {
			if (string.startsWith("[*.{") && !string.contains(",")) {
				String suffix = string.substring(
						string.indexOf("{") + 1,
						string.indexOf("}")
				);
				return "[*." + suffix + "]";
			}
			return string;
		}).toList();
	}

}
