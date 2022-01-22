package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class ReadmeChore {

	private static String DEFAULT_README = """
			# ${PROJECT}

			Short description

			# Quickstart

			`./call-some-command --with-some-options`

			# Install

			`./install`

			""";

	private final ChoreContext context;

	public ReadmeChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path readmeMd = context.resolve("README.md");
		Path readme = context.resolve("README");
		Path readmeTxt = context.resolve("README.txt");
		Path readmeMarkdown = context.resolve("README.markdown");
		if (FilesSilent
				.noneExists(readmeMd, readme, readmeTxt, readmeMarkdown)) {
			FilesSilent.writeString(readmeMd, DEFAULT_README);
		}
	}

}
