package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;

public class IdiomaticVersionFileChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.javaDirs(context).forEach(javaDir -> {
			Path javaVersion = javaDir.resolve(".java-version");
			if (!FilesSilent.exists(javaVersion)) {
				var version = "temurin-25\n";
				if (FilesSilent.readString(javaDir.resolve("pom.xml"))
						.contains("native-maven-plugin")) {
					version = "graalvm-community-25.0.0\n";
				}
				FilesSilent.writeString(javaVersion, version);
			}

		});
		return context;
	}

}
