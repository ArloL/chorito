package io.github.arlol.chorito.chores;

import java.util.Arrays;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class DeleteUnwantedFilesChore implements Chore {

	private static final List<String> PROHIBITED_FILES = Arrays
			.asList(".DS_Store");

	@Override
	public ChoreContext doit(ChoreContext context) {
		context.files().forEach(path -> {
			String filename = MyPaths.getFileName(path).toString();
			if (PROHIBITED_FILES.contains(filename)) {
				FilesSilent.deleteIfExists(path);
			}
		});
		return context;
	}

}
