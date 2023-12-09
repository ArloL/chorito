package io.github.arlol.chorito.chores;

import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.Arrays;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class EditorConfigChore implements Chore {

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
	private static String DEFAULT_POM_XML = """

			[pom.xml]
			indent_style = tab
			""";

	@Override
	public void doit(ChoreContext context) {
		Path editorConfigPath = context.resolve(".editorconfig");
		if (!FilesSilent.exists(editorConfigPath)) {
			FilesSilent.writeString(editorConfigPath, DEFAULT_EDITORCONFIG);
		}
		var content = FilesSilent.readString(editorConfigPath).trim() + "\n";
		if (!content.contains("[.vscode/**.json]")) {
			Path vsCodeLocation = context.resolve(".vscode");
			if (context.textFiles().stream().anyMatch(path -> {
				if (path.startsWith(vsCodeLocation)) {
					return path.toString().endsWith(".json");
				}
				return false;
			})) {
				content += DEFAULT_VSCODE_EDITORCONFIG;
			}
		}
		if (!content.contains("[.idea/**]")) {
			Path vsCodeLocation = context.resolve(".idea");
			if (context.textFiles().stream().anyMatch(path -> {
				if (path.startsWith(vsCodeLocation)) {
					return true;
				}
				return false;
			})) {
				content += DEFAULT_IDEA_EDITORCONFIG;
			}
		}
		if (!content.contains("[pom.xml]")) {
			Path pom = context.resolve("pom.xml");
			if (FilesSilent.exists(pom)) {
				content += DEFAULT_POM_XML;
			}
		}
		content = removeBracketsFromSingleExtensionGroups(content);
		FilesSilent.writeString(editorConfigPath, content);
	}

	public static String removeBracketsFromSingleExtensionGroups(
			String content
	) {
		// make sure single file name matches are without brackets
		return Arrays.stream(content.split("\r?\n")).map(string -> {
			if (string.startsWith("[*.{") && !string.contains(",")) {
				String suffix = string.substring(
						string.indexOf("{") + 1,
						string.indexOf("}")
				);
				return "[*." + suffix + "]";
			}
			return string;
		}).collect(joining("\n", "", "\n"));
	}

}
