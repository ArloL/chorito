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

	@Override
	public ChoreContext doit(ChoreContext context) {
		context.textFiles()
				.stream()
				.filter(
						file -> file.endsWith("Dockerfile")
								|| file.endsWith(".dockerignore")
				)
				.map(MyPaths::getParent)
				.forEach(dir -> {
					var newContent = new StringBuilder(DOCKERIGNORE_DEFAULT);
					for (String compose : List.of(
							"compose.yaml",
							"compose.yml",
							"docker-compose.yml",
							"docker-compose.yaml"
					)) {
						if (FilesSilent.exists(dir.resolve(compose))) {
							newContent.append(compose);
							newContent.append("\n");
						}
					}

					String mavenDirs = DirectoryStreams.mavenPoms(context)
							.map(MyPaths::getParent)
							.filter(path -> path.startsWith(dir))
							.map(path -> dir.relativize(path))
							.map(Path::toString)
							.map(path -> path.isBlank() ? "" : "/" + path)
							.map(path -> path + "/target/")
							.collect(joining("\n", "\n", "\n"));
					if (!mavenDirs.isBlank()) {
						newContent.append("\n### Maven ###\n");
						newContent.append(mavenDirs);
					}

					String gradleDirs = DirectoryStreams.gradleDirs(context)
							.filter(path -> path.startsWith(dir))
							.map(path -> dir.relativize(path))
							.map(Path::toString)
							.map(path -> path.isBlank() ? "" : "/" + path)
							.flatMap(
									path -> Stream.of(
											path + "/build/",
											path + "/.gradle/"
									)
							)
							.collect(joining("\n", "\n", "\n"));
					if (!gradleDirs.isBlank()) {
						newContent.append("\n### Gradle ###\n");
						newContent.append(gradleDirs);
					}

					String nodeDirs = DirectoryStreams.packageJsonDirs(context)
							.filter(pom -> pom.startsWith(dir))
							.map(path -> dir.relativize(path))
							.map(Path::toString)
							.map(path -> path.isBlank() ? "" : "/" + path)
							.map(path -> path + "/node_modules/")
							.collect(joining("\n", "\n", "\n"));
					if (!nodeDirs.isBlank()) {
						newContent.append("\n### node ###\n");
						newContent.append(nodeDirs);
					}

					ExistingFileUpdater.update(
							dir.resolve(".dockerignore"),
							newContent.toString()
					);
				});
		return context;
	}

}
