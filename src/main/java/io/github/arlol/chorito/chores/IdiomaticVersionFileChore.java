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
				FilesSilent.writeString(
						toolVersions,
						"java " + defaultVendor(javaDir) + "\n"
				);
				return;
			}
			var content = FilesSilent.readString(toolVersions);
			var pinned = pin(content);
			if (content.lines().noneMatch(l -> l.startsWith("java "))) {
				pinned = pinned + "java " + defaultVendor(javaDir) + "\n";
			}
			if (!pinned.equals(content)) {
				FilesSilent.writeString(toolVersions, pinned);
			}
		});
		// A root .tool-versions is what mise-action reads, even when only a
		// subdirectory is a Java dir.
		Path root = context.resolve(".tool-versions");
		if (FilesSilent.exists(root)) {
			var content = FilesSilent.readString(root);
			var pinned = pin(content);
			if (!pinned.equals(content)) {
				FilesSilent.writeString(root, pinned);
			}
		}
		return context;
	}

	private static String defaultVendor(Path javaDir) {
		Path pom = javaDir.resolve("pom.xml");
		return FilesSilent.exists(pom)
				&& FilesSilent.readString(pom).contains("native-maven-plugin")
						? "graalvm-community-innovation-" + JavaVersions.GRAALVM
						: "temurin-" + JavaVersions.TEMURIN;
	}

	private static String pin(String content) {
		if (content.isEmpty()) {
			return content;
		}
		return content.lines()
				.map(IdiomaticVersionFileChore::pinJavaLine)
				.collect(Collectors.joining("\n", "", "\n"));
	}

	static String pinJavaLine(String line) {
		if (line.equals("java temurin-25")) {
			return "java temurin-" + JavaVersions.TEMURIN;
		}
		if (line.matches("java graalvm-community-\\d[\\d.]*")) {
			return "java graalvm-community-innovation-" + JavaVersions.GRAALVM;
		}
		return line;
	}

}
