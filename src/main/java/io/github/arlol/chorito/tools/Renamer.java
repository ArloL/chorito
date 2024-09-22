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
		String fileName = MyPaths.getFileNameAsString(path);
		FilesSilent.move(
				path,
				path.resolveSibling(fileName.replace(target, replacement))
		);
	}

}
