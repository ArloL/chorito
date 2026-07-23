package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;

public class NpmrcChore implements Chore {

	private static final String MIN_RELEASE_AGE = "min-release-age";

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.packageJsonDirs(context).forEach(dir -> {
			Path npmrc = dir.resolve(".npmrc");
			if (!FilesSilent.exists(npmrc)) {
				FilesSilent.writeString(npmrc, MIN_RELEASE_AGE + "=7\n");
				return;
			}
			List<String> lines = FilesSilent.readAllLines(npmrc);
			boolean changed = false;
			boolean found = false;
			for (int i = 0; i < lines.size(); i++) {
				String[] keyValue = lines.get(i).split("=", 2);
				if (!keyValue[0].strip().equals(MIN_RELEASE_AGE)) {
					continue;
				}
				found = true;
				if (keyValue.length == 2 && isAtLeastSeven(keyValue[1])) {
					continue;
				}
				lines.set(i, MIN_RELEASE_AGE + "=7");
				changed = true;
			}
			if (!found) {
				lines.add(MIN_RELEASE_AGE + "=7");
				changed = true;
			}
			if (changed) {
				FilesSilent.write(npmrc, lines, "\n");
			}
		});
		return context;
	}

	private boolean isAtLeastSeven(String value) {
		try {
			return Integer.parseInt(value.strip()) >= 7;
		} catch (NumberFormatException e) {
			return true;
		}
	}

}
