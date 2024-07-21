package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;

public class IntellijChore implements Chore {

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
			overwriteFromTemplate(context, "eclipseCodeFormatter");
			overwriteFromTemplate(context, "externalDependencies");
			overwriteFromTemplate(context, "saveactions_settings");
			overwriteFromTemplate(context, "codeStyles/codeStyleConfig");
			overwriteFromTemplate(context, "codeStyles/Project");
			return context.refresh();
		}
		return context;
	}

	private void overwriteFromTemplate(ChoreContext context, String name) {
		Path path = context.resolve(".idea/" + name + ".xml");
		String template = ClassPathFiles
				.readString("idea-settings/" + name + ".xml");
		FilesSilent.writeString(path, template);
	}

}
