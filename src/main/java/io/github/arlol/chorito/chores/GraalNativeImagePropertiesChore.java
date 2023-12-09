package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class GraalNativeImagePropertiesChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path metaInfNativeImage = context
				.resolve("src/main/resources/META-INF/native-image/");
		if (FilesSilent.exists(metaInfNativeImage)) {
			FilesSilent.walk(metaInfNativeImage)
					.filter(p -> p.endsWith("native-image.properties"))
					.forEach(p -> {
						String content = FilesSilent.readString(p);
						content = content
								.replace("--allow-incomplete-classpath", "");
						content = content.replace("\n \\\n", "\n");
						FilesSilent.writeString(p, content);
					});
		}
		return context;
	}

}
