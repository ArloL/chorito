package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitIgnoreChore implements Chore {

	@Override
	public void doit(ChoreContext context) {
		Path gitignore = context.resolve(".gitignore");
		if (FilesSilent.exists(context.resolve("pom.xml"))) {

			List<String> lines;
			if (FilesSilent.exists(gitignore)) {
				lines = new ArrayList<>(FilesSilent.readAllLines(gitignore));

				if (!lines.contains(".project")) {
					lines.add(".project");
				}
				lines = lines.stream().map(s -> {
					if (s.equals(".settings")) {
						return "# .settings";
					}
					if (s.equals(".settings/")) {
						return "# .settings/";
					}

					return s;
				}).toList();

			} else {
				lines = List.of(".project");
			}
			FilesSilent.write(gitignore, lines, "\n");

			Path settingsGitignore = context.resolve(".settings/.gitignore");
			String currentGitignore = ClassPathFiles.readString("/.gitignore");
			FilesSilent.writeString(settingsGitignore, currentGitignore);
		}
	}

}
