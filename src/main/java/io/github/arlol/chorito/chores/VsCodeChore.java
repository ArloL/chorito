package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;

public class VsCodeChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		if (FilesSilent.exists(context.resolve("pom.xml"))) {
			Path settings = context.resolve(".vscode/settings.json");
			if (!FilesSilent.exists(settings)) {
				String templateSettings = ClassPathFiles
						.readString("/settings.json");
				FilesSilent.writeString(settings, templateSettings);
			}

			Path extensions = context.resolve(".vscode/extensions.json");
			if (!FilesSilent.exists(extensions)) {
				String templateExtensions = ClassPathFiles
						.readString("/extensions.json");
				FilesSilent.writeString(extensions, templateExtensions);
			}
		}
		return context;
	}

}
