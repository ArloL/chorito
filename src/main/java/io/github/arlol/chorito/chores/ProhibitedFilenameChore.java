package io.github.arlol.chorito.chores;

import java.util.Arrays;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class ProhibitedFilenameChore {

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

	private final ChoreContext context;

	public ProhibitedFilenameChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		context.files().stream().forEach(path -> {
			String filenameNoExtension = path.getFileName().toString();
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
