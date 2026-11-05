package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.stream.Collectors;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JavaVersions;

public class IdiomaticVersionFileChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.rootJavaDirs(context).forEach(javaDir -> {
			FilesSilent.deleteIfExists(javaDir.resolve(".java-version"));
			Path toolVersions = javaDir.resolve(".tool-versions");
			if (!FilesSilent.exists(toolVersions)) {
				var vendor = FilesSilent.readString(javaDir.resolve("pom.xml"))
						.contains("native-maven-plugin")
								? "graalvm-community-innovation-"
										+ JavaVersions.GRAALVM
								: "temurin-" + JavaVersions.TEMURIN;
				FilesSilent.writeString(toolVersions, "java " + vendor + "\n");
				return;
			}
			var content = FilesSilent.readString(toolVersions);
			var pinned = content.lines()
					.map(IdiomaticVersionFileChore::pinJavaLine)
					.collect(Collectors.joining("\n", "", "\n"));
			if (!content.isEmpty() && !pinned.equals(content)) {
				FilesSilent.writeString(toolVersions, pinned);
			}
		});
		return context;
	}

	static String pinJavaLine(String line) {
		if (line.equals("java temurin-25")) {
			return "java temurin-" + JavaVersions.TEMURIN;
		}
		if (line.matches("java graalvm-community-\\d+(\\.\\d+)*")) {
			return "java graalvm-community-innovation-" + JavaVersions.GRAALVM;
		}
		return line;
	}

}
