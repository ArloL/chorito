package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class DockerIgnoreChore implements Chore {

	private static String DOCKERIGNORE_PREFIX = """
			# Created by chorito https://github.com/ArloL/chorito
			""";
	private static String DOCKERIGNORE_SUFFIX = """
			# End of chorito. Add your ignores after this line and they will be preserved.""";

	private static String DOCKERIGNORE_DEFAULT = """
			### Docker ###

			Dockerfile
			.dockerignore
			""";

	private static String DOCKERIGNORE_MAVEN = """
			### Maven ###

			/target/
			""";

	private static String DOCKERIGNORE_GRADLE = """
			### Gradle ###

			/build/
			/.gradle/
			""";
	private static String DOCKERIGNORE_NODE = """
			### node ###

			/node_modules/
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
					String newDockerignoreContent = DOCKERIGNORE_PREFIX;
					newDockerignoreContent += "\n" + DOCKERIGNORE_DEFAULT;
					for (String compose : List.of(
							"compose.yaml",
							"compose.yml",
							"docker-compose.yml",
							"docker-compose.yaml"
					)) {
						if (FilesSilent.exists(dir.resolve(compose))) {
							newDockerignoreContent += compose + "\n";
						}
					}
					if (FilesSilent.anyChildExists(dir, "mvnw", "pom.xml")) {
						newDockerignoreContent += "\n" + DOCKERIGNORE_MAVEN;
					}
					if (FilesSilent
							.anyChildExists(dir, "gradlew", "build.gradle")) {
						newDockerignoreContent += "\n" + DOCKERIGNORE_GRADLE;
					}
					if (FilesSilent.anyChildExists(dir, "package.json")) {
						newDockerignoreContent += "\n" + DOCKERIGNORE_NODE;
					}
					newDockerignoreContent += "\n" + DOCKERIGNORE_SUFFIX;

					Path dockerignore = dir.resolve(".dockerignore");
					if (!FilesSilent.exists(dockerignore)) {
						FilesSilent.writeString(
								dockerignore,
								newDockerignoreContent
						);
					}
					List<String> currentDockerignore = FilesSilent
							.readAllLines(dockerignore);
					if (currentDockerignore.isEmpty()
							|| !currentDockerignore.get(0)
									.startsWith("# Created by chorito")) {
						FilesSilent.writeString(
								dockerignore,
								newDockerignoreContent
						);
						currentDockerignore = FilesSilent
								.readAllLines(dockerignore);
					}
					int endOfChorito = currentDockerignore.indexOf(
							"# End of chorito. Add your ignores after this line and they will be preserved."
					) + 1;
					currentDockerignore = currentDockerignore
							.subList(endOfChorito, currentDockerignore.size());
					currentDockerignore.add(0, newDockerignoreContent);
					FilesSilent.write(dockerignore, currentDockerignore, "\n");
				});
		return context;
	}

}
