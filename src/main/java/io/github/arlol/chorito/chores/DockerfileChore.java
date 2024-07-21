package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class DockerfileChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		context.textFiles().forEach(textFile -> {
			String filename = MyPaths.getFileName(textFile).toString();
			if (filename.equalsIgnoreCase("dockerfile")
					&& !filename.equals("Dockerfile")) {
				FilesSilent
						.move(textFile, textFile.resolveSibling("Dockerfile"));
			}
		});
		return context.refresh();
	}

}
