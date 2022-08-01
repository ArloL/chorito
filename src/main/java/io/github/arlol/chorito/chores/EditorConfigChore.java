package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.nio.file.Paths;
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
	private static String DEFAULT_VSCODE_EDITORCONFIG = """

			[.vscode/**.json]
			insert_final_newline = false
			""";
	private static String DEFAULT_IDEA_EDITORCONFIG = """

			[.idea/**]
			insert_final_newline = false
			""";

	private final ChoreContext context;

	public EditorConfigChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path editorConfigPath = context.resolve(".editorconfig");
		if (!FilesSilent.exists(editorConfigPath)) {
			FilesSilent.writeString(editorConfigPath, DEFAULT_EDITORCONFIG);
		}
		var content = FilesSilent.readAllLines(editorConfigPath);
		if (!content.contains("[.vscode/**.json]")) {
			Path vsCodeLocation = Paths.get(".vscode");
			if (context.files().stream().anyMatch(path -> {
				if (path.startsWith(vsCodeLocation)) {
					return path.toString().endsWith(".json");
				}
				return false;
			})) {
				content.add(DEFAULT_VSCODE_EDITORCONFIG);
			}
		}
		if (!content.contains("[.idea/**]")) {
			Path vsCodeLocation = Paths.get(".idea");
			if (context.files().stream().anyMatch(path -> {
				if (path.startsWith(vsCodeLocation)) {
					return true;
				}
				return false;
			})) {
				content.add(DEFAULT_IDEA_EDITORCONFIG);
			}
		}
		content = removeBracketsFromSingleExtensionGroups(content);
		FilesSilent.write(editorConfigPath, content);
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
