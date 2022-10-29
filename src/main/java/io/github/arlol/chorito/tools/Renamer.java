package io.github.arlol.chorito.tools;

import java.nio.file.Path;

public abstract class Renamer {

	private Renamer() {
	}

	public static void replaceInFilename(
			Path path,
			String target,
			String replacement
	) {
		Path fileName = MyPaths.getFileName(path);
		FilesSilent.move(
				path,
				path.resolveSibling(
						fileName.toString().replace(target, replacement)
				)
		);
	}

}
