package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;

public class IntellijChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		boolean changed = false;
		if (FilesSilent.exists(context.resolve("pom.xml"))) {
			Path externalDependencies = context
					.resolve(".idea/externalDependencies.xml");
			String templateExternalDependencies = ClassPathFiles
					.readString("/externalDependencies.xml");
			FilesSilent.writeString(
					externalDependencies,
					templateExternalDependencies
			);

			Path eclipseCodeFormatter = context
					.resolve(".idea/eclipseCodeFormatter.xml");
			String templateEclipseCodeFormatter = ClassPathFiles
					.readString("/eclipseCodeFormatter.xml");
			FilesSilent.writeString(
					eclipseCodeFormatter,
					templateEclipseCodeFormatter
			);
			changed = true;
		}
		if (changed) {
			context = context.refresh();
		}
		return context;
	}

}
