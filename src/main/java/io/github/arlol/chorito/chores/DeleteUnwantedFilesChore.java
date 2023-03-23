package io.github.arlol.chorito.chores;

import java.util.Arrays;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class DeleteUnwantedFilesChore {

	private static final List<String> PROHIBITED_FILES = Arrays
			.asList(".DS_Store");

	private final ChoreContext context;

	public DeleteUnwantedFilesChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		context.files().forEach(path -> {
			String filename = MyPaths.getFileName(path).toString();
			if (PROHIBITED_FILES.contains(filename)) {
				FilesSilent.deleteIfExists(path);
			}
		});
	}

}
