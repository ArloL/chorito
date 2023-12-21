package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;

public class VsCodeChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		boolean changed = false;
		if (FilesSilent.exists(context.resolve("pom.xml"))) {
			Path settings = context.resolve(".vscode/settings.json");
			String templateSettings = ClassPathFiles
					.readString("vscode-settings/settings.json");
			if (!FilesSilent.exists(settings)) {
				FilesSilent.writeString(settings, templateSettings);
				changed = true;
			}

			Path extensions = context.resolve(".vscode/extensions.json");
			String templateExtensions = ClassPathFiles
					.readString("vscode-settings/extensions.json");
			if (!FilesSilent.exists(extensions)) {
				FilesSilent.writeString(extensions, templateExtensions);
				changed = true;
			}
		}
		if (changed) {
			context = context.refresh();
		}
		return context;
	}

}
