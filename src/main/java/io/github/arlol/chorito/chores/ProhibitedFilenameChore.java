package io.github.arlol.chorito.chores;

import java.util.Arrays;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class ProhibitedFilenameChore implements Chore {

	private static final List<String> PROHIBITED_FILENAMES = Arrays.asList(
			"CON",
			"PRN",
			"AUX",
			"NUL",
			"COM1",
			"COM2",
			"COM3",
			"COM4",
			"COM5",
			"COM6",
			"COM7",
			"COM8",
			"COM9",
			"LPT1",
			"LPT2",
			"LPT3",
			"LPT4",
			"LPT5",
			"LPT6",
			"LPT7",
			"LPT8",
			"LPT9"
	);;

	@Override
	public void doit(ChoreContext context) {
		context.files().forEach(path -> {
			String filenameNoExtension = MyPaths.getFileName(path).toString();
			if (filenameNoExtension.contains(".")) {
				filenameNoExtension = filenameNoExtension
						.substring(0, filenameNoExtension.lastIndexOf('.'));
			}
			if (PROHIBITED_FILENAMES.contains(filenameNoExtension)) {
				FilesSilent.deleteIfExists(path);
			}
		});
	}

}
