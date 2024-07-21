package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;

public class VsCodeChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		boolean hasPom = context.textFiles()
				.stream()
				.anyMatch(file -> file.endsWith("pom.xml"));
		boolean hasMvnw = context.textFiles()
				.stream()
				.anyMatch(file -> file.endsWith("mvnw"));
		boolean hasGradlew = context.textFiles()
				.stream()
				.anyMatch(file -> file.endsWith("gradlew"));
		if (hasPom || hasMvnw || hasGradlew) {
			Path settings = context.resolve(".vscode/settings.json");
			String templateSettings = ClassPathFiles
					.readString("vscode-settings/settings.json");
			FilesSilent.writeString(settings, templateSettings);

			Path extensions = context.resolve(".vscode/extensions.json");
			String templateExtensions = ClassPathFiles
					.readString("vscode-settings/extensions.json");
			FilesSilent.writeString(extensions, templateExtensions);
			return context.refresh();
		}
		return context;
	}

}
