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
			Path externalDependencies = context
					.resolve(".idea/externalDependencies.xml");
			String templateExternalDependencies = ClassPathFiles
					.readString("idea-settings/externalDependencies.xml");
			FilesSilent.writeString(
					externalDependencies,
					templateExternalDependencies
			);

			Path eclipseCodeFormatter = context
					.resolve(".idea/eclipseCodeFormatter.xml");
			String templateEclipseCodeFormatter = ClassPathFiles
					.readString("idea-settings/eclipseCodeFormatter.xml");
			FilesSilent.writeString(
					eclipseCodeFormatter,
					templateEclipseCodeFormatter
			);

			Path saveactionsSettings = context
					.resolve(".idea/saveactions_settings.xml");
			String templateSaveactionsSettings = ClassPathFiles
					.readString("idea-settings/saveactions_settings.xml");
			FilesSilent.writeString(
					saveactionsSettings,
					templateSaveactionsSettings
			);

			return context.refresh();
		}
		return context;
	}

}
