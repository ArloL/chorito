package io.github.arlol.chorito.chores;

import java.util.List;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ExistingFileUpdater;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class DockerIgnoreChore implements Chore {

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
					String newContent = DOCKERIGNORE_DEFAULT;
					for (String compose : List.of(
							"compose.yaml",
							"compose.yml",
							"docker-compose.yml",
							"docker-compose.yaml"
					)) {
						if (FilesSilent.exists(dir.resolve(compose))) {
							newContent += compose + "\n";
						}
					}
					if (FilesSilent.anyChildExists(dir, "mvnw", "pom.xml")) {
						newContent += "\n" + DOCKERIGNORE_MAVEN;
					}
					if (FilesSilent
							.anyChildExists(dir, "gradlew", "build.gradle")) {
						newContent += "\n" + DOCKERIGNORE_GRADLE;
					}
					if (FilesSilent.anyChildExists(dir, "package.json")) {
						newContent += "\n" + DOCKERIGNORE_NODE;
					}

					ExistingFileUpdater
							.update(dir.resolve(".dockerignore"), newContent);
				});
		return context;
	}

}
