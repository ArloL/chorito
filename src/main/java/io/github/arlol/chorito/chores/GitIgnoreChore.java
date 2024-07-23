package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import io.github.arlol.chorito.filter.FileHasNoParentDirectoryWithFileFilter;
import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class GitIgnoreChore implements Chore {

	private static String GITIGNORE_PREFIX = """
			# Created by chorito https://github.com/ArloL/chorito
			""";

	private static String GITIGNORE_ECLIPSE = """
			### Eclipse ###

			/.project
			""";

	private static String GITIGNORE_ECLIPSE_JAVA = """
			### Eclipse+Java ###

			/.classpath
			""";

	private static String GITIGNORE_MAVEN = """
			### Maven ###

			/target/
			/.flattened-pom.xml
			""";

	private static String GITIGNORE_SUFFIX = """
			# End of chorito. Add your ignores after this line and they will be preserved.""";

	private static String GITIGNORE_GRADLE = """
			### Gradle ###

			/.gradle/
			/build/
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
		createMavenAndGradleIgnore(context);
		createEclipseSettingsIgnore(context);
		createVscodeSettingsIgnore(context);
		createIdeaSettingsIgnore(context);
		return context.refresh();
	}

	private void updateExistingGitignore(
			Path gitignore,
			String newGitignoreContent
	) {
		if (!FilesSilent.exists(gitignore)) {
			FilesSilent.writeString(gitignore, newGitignoreContent);
		}
		List<String> currentGitignore = FilesSilent.readAllLines(gitignore);
		if (currentGitignore.isEmpty() || !currentGitignore.get(0)
				.startsWith("# Created by chorito")) {
			FilesSilent.writeString(gitignore, newGitignoreContent);
			currentGitignore = FilesSilent.readAllLines(gitignore);
		}
		int endOfChorito = currentGitignore.indexOf(
				"# End of chorito. Add your ignores after this line and they will be preserved."
		) + 1;
		currentGitignore = currentGitignore
				.subList(endOfChorito, currentGitignore.size());
		currentGitignore.add(0, newGitignoreContent);
		FilesSilent.write(gitignore, currentGitignore, "\n");
	}

	private void createMavenAndGradleIgnore(ChoreContext context) {
		Stream.of(
				context.textFiles()
						.stream()
						.filter(file -> file.endsWith("pom.xml"))
						.map(MyPaths::getParent)
						.filter(
								file -> FileHasNoParentDirectoryWithFileFilter
										.filter(file, "pom.xml")
						),
				context.textFiles()
						.stream()
						.map(MyPaths::getParent)
						.filter(
								file -> FilesSilent.anyChildExists(
										file,
										"mvnw",
										"gradlew",
										"build.gradle"
								)
						)
		).flatMap(Function.identity()).forEach(dir -> {
			String newGitignoreContent = GITIGNORE_PREFIX;
			newGitignoreContent += "\n" + GITIGNORE_ECLIPSE;
			if (FilesSilent.anyChildExists(dir, "mvnw", "pom.xml")) {
				newGitignoreContent += "\n" + GITIGNORE_ECLIPSE_JAVA;
				newGitignoreContent += "\n" + GITIGNORE_MAVEN;
			}
			if (FilesSilent.anyChildExists(dir, "gradlew", "build.gradle")) {
				newGitignoreContent += "\n" + GITIGNORE_ECLIPSE_JAVA;
				newGitignoreContent += "\n" + GITIGNORE_GRADLE;
			}
			newGitignoreContent += "\n" + GITIGNORE_SUFFIX;
			updateExistingGitignore(
					dir.resolve(".gitignore"),
					newGitignoreContent
			);
		});
	}

	private void createEclipseSettingsIgnore(ChoreContext context) {
		Stream.of(
				context.textFiles()
						.stream()
						.filter(file -> file.endsWith("pom.xml"))
						.map(MyPaths::getParent)
						.filter(
								file -> FileHasNoParentDirectoryWithFileFilter
										.filter(file, "pom.xml")
						),
				context.textFiles()
						.stream()
						.filter(
								file -> file.endsWith(
										"org.eclipse.jdt.core.prefs"
								) || file.endsWith("org.eclipse.jdt.ui.prefs")
						)
						.map(MyPaths::getParent)
						.filter(file -> file.endsWith(".settings"))
						.map(MyPaths::getParent)
		).flatMap(Function.identity()).forEach(dir -> {
			Path settingsGitignore = dir.resolve(".settings/.gitignore");
			String templateGitignore = ClassPathFiles
					.readString("eclipse-settings/.gitignore");
			updateExistingGitignore(settingsGitignore, templateGitignore);
		});
	}

	private void createVscodeSettingsIgnore(ChoreContext context) {
		Stream.of(
				context.textFiles()
						.stream()
						.map(MyPaths::getParent)
						.filter(file -> file.endsWith(".vscode"))
						.map(MyPaths::getParent)
		).flatMap(Function.identity()).forEach(dir -> {
			Path settingsGitignore = dir.resolve(".vscode/.gitignore");
			String templateGitignore = ClassPathFiles
					.readString("vscode-settings/.gitignore");
			updateExistingGitignore(settingsGitignore, templateGitignore);
		});
	}

	private void createIdeaSettingsIgnore(ChoreContext context) {
		Stream.of(
				context.textFiles()
						.stream()
						.map(MyPaths::getParent)
						.filter(file -> file.endsWith(".idea"))
						.map(MyPaths::getParent)
		).flatMap(Function.identity()).forEach(dir -> {
			Path settingsGitignore = dir.resolve(".idea/.gitignore");
			String templateGitignore = ClassPathFiles
					.readString("idea-settings/.gitignore");
			updateExistingGitignore(settingsGitignore, templateGitignore);
		});
	}

}
