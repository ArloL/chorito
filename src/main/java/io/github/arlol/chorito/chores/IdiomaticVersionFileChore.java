package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;

public class IdiomaticVersionFileChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.javaDirs(context).forEach(javaDir -> {
			FilesSilent.deleteIfExists(javaDir.resolve(".java-version"));
			Path toolVersions = javaDir.resolve(".tool-versions");
			if (!FilesSilent.exists(toolVersions)) {
				var version = "java temurin-25\n";
				if (FilesSilent.readString(javaDir.resolve("pom.xml"))
						.contains("native-maven-plugin")) {
					version = "java graalvm-community-25\n";
				}
				FilesSilent.writeString(toolVersions, version);
			}

		});
		return context;
	}

}
