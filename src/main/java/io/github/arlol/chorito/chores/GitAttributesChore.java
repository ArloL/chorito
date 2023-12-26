package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitAttributesChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path path = context.resolve(".gitattributes");

		List<String> lines = new ArrayList<>();
		if (FilesSilent.exists(path)) {
			lines = FilesSilent.readAllLines(path);
		}
		if (lines.stream().noneMatch(s -> s.startsWith("* "))) {
			lines.add("*        text=auto eol=lf");
		}
		if (lines.stream().noneMatch(s -> s.startsWith("*.bat "))) {
			lines.add("*.bat    text      eol=crlf");
		}
		if (lines.stream().noneMatch(s -> s.startsWith("*.cmd "))) {
			lines.add("*.cmd    text      eol=crlf");
		}
		if (lines.stream().noneMatch(s -> s.startsWith("*.ps1 "))) {
			lines.add("*.ps1    text      eol=crlf");
		}
		if (lines.stream().noneMatch(s -> s.startsWith("*.sh "))) {
			lines.add("*.sh     text      eol=lf");
		}

		FilesSilent.write(path, lines, "\n");

		return context;
	}

}
