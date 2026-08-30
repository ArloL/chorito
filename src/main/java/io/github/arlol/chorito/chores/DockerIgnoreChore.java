package io.github.arlol.chorito.chores;

import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.ExistingFileUpdater;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class DockerIgnoreChore implements Chore {

	private static String DOCKERIGNORE_DEFAULT = """
			### Docker ###

			Dockerfile
			.dockerignore
			""";

	private static final List<String> COMPOSE_FILENAMES = List.of(
			"compose.yaml",
			"compose.yml",
			"docker-compose.yml",
			"docker-compose.yaml"
	);

	@Override
	public ChoreContext doit(ChoreContext context) {
		context.textFiles()
				.stream()
				.filter(
						file -> file.endsWith("Dockerfile")
								|| file.endsWith(".dockerignore")
				)
				.map(MyPaths::getParent)
				.forEach(dir -> updateDockerIgnore(context, dir));
		return context;
	}

	private void updateDockerIgnore(ChoreContext context, Path dir) {
		var newContent = new StringBuilder(DOCKERIGNORE_DEFAULT);
		appendComposeFiles(newContent, dir);
		appendSection(newContent, "Maven", mavenTargetDirs(context, dir));
		appendSection(newContent, "Gradle", gradleBuildDirs(context, dir));
		appendSection(newContent, "node", nodeModulesDirs(context, dir));
		ExistingFileUpdater
				.update(dir.resolve(".dockerignore"), newContent.toString());
	}

	private static void appendComposeFiles(StringBuilder content, Path dir) {
		for (String compose : COMPOSE_FILENAMES) {
			if (FilesSilent.exists(dir.resolve(compose))) {
				content.append(compose);
				content.append("\n");
			}
		}
	}

	private static void appendSection(
			StringBuilder content,
			String title,
			String entries
	) {
		if (entries.isBlank()) {
			return;
		}
		content.append("\n### ").append(title).append(" ###\n");
		content.append(entries);
	}

	private static String mavenTargetDirs(ChoreContext context, Path dir) {
		return DirectoryStreams.mavenPoms(context)
				.map(MyPaths::getParent)
				.filter(path -> path.startsWith(dir))
				.map(path -> relativeTo(dir, path))
				.map(path -> path + "/target/")
				.collect(joining("\n", "\n", "\n"));
	}

	private static String gradleBuildDirs(ChoreContext context, Path dir) {
		return DirectoryStreams.gradleDirs(context)
				.filter(path -> path.startsWith(dir))
				.map(path -> relativeTo(dir, path))
				.flatMap(
						path -> Stream.of(path + "/build/", path + "/.gradle/")
				)
				.collect(joining("\n", "\n", "\n"));
	}

	private static String nodeModulesDirs(ChoreContext context, Path dir) {
		return DirectoryStreams.packageJsonDirs(context)
				.filter(path -> path.startsWith(dir))
				.map(path -> relativeTo(dir, path))
				.map(path -> path + "/node_modules/")
				.collect(joining("\n", "\n", "\n"));
	}

	/**
	 * The empty string for {@code dir} itself, "/sub" for anything below it.
	 */
	private static String relativeTo(Path dir, Path path) {
		String relative = dir.relativize(path).toString();
		return relative.isBlank() ? "" : "/" + relative;
	}

}
