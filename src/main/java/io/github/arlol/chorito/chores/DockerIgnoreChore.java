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
			Dockerfile
			.dockerignore
			""";

	private static String DOCKERIGNORE_MAVEN = """
			target/
			""";

	private static String DOCKERIGNORE_GRADLE = """
			build/
			.gradle/
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
		context.textFiles()
				.stream()
				.filter(file -> file.endsWith("Dockerfile"))
				.map(MyPaths::getParent)
				.forEach(dockerDir -> {
					Path dockerignore = dockerDir.resolve(".dockerignore");
					Path pomXml = dockerDir.resolve("pom.xml");
					Path buildGradle = dockerDir.resolve("build.gradle");

					String newDockerignoreContent = DOCKERIGNORE_PREFIX;
					newDockerignoreContent += "\n" + DOCKERIGNORE_DEFAULT;
					if (FilesSilent.exists(pomXml)) {
						newDockerignoreContent += "\n" + DOCKERIGNORE_MAVEN;
					}
					if (FilesSilent.exists(buildGradle)) {
						newDockerignoreContent += "\n" + DOCKERIGNORE_GRADLE;
					}
					newDockerignoreContent += "\n" + DOCKERIGNORE_SUFFIX;

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
