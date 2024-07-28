package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import io.github.arlol.chorito.tools.ChoreContext;
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
			/bin/
			""";

	private static String GITIGNORE_IDEA = """
			### IntelliJ IDEA ###

			/out/
			""";

	private static String GITIGNORE_MAVEN = """
			### Maven ###

			/target/
			/.flattened-pom.xml
			""";

	private static String GITIGNORE_MAVEN_WRAPPER = """
			### Maven Wrapper ###

			!/maven-wrapper.jar
			!/maven-wrapper.properties

			""";

	private static String GITIGNORE_GRADLE = """
			### Gradle ###

			/.gradle/
			/build/
			""";

	private static String GITIGNORE_GRADLE_WRAPPER = """
			### Gradle Wrapper ###

			!/gradle-wrapper.jar
			!/gradle-wrapper.properties

			""";

	private static String GITIGNORE_ECLIPSE_SETTINGS = """
			*.prefs
			!org.eclipse.jdt.core.prefs
			!org.eclipse.jdt.ui.prefs
			""";

	private static String GITIGNORE_IDEA_SETTINGS = """
			*
			!/.gitignore
			!/eclipseCodeFormatter.xml
			!/externalDependencies.xml
			!/saveactions_settings.xml
			!/codeStyles
			!/codeStyles/codeStyleConfig.xml
			!/codeStyles/Project.xml
			""";

	private static String GITIGNORE_VSCODE_SETTINGS = """
			*
			!.gitignore
			!settings.json
			!tasks.json
			!launch.json
			!extensions.json
			!*.code-snippets
			""";

	private static String GITIGNORE_SUFFIX = """
			# End of chorito. Add your ignores after this line and they will be preserved.
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
		createMavenAndGradleIgnore(context);
		createMavenWrapperIgnore(context);
		createGradleWrapperIgnore(context);
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
		context.textFiles()
				.stream()
				.filter(
						file -> file.endsWith("pom.xml")
								|| file.endsWith("mvnw")
								|| file.endsWith("gradlew")
								|| file.endsWith("build.gradle")
				)
				.map(MyPaths::getParent)
				.forEach(dir -> {
					String newGitignoreContent = GITIGNORE_PREFIX;
					newGitignoreContent += "\n" + GITIGNORE_ECLIPSE;
					if (FilesSilent.anyChildExists(dir, "mvnw", "pom.xml")) {
						newGitignoreContent += "\n" + GITIGNORE_ECLIPSE_JAVA;
						newGitignoreContent += "\n" + GITIGNORE_IDEA;
						newGitignoreContent += "\n" + GITIGNORE_MAVEN;
					}
					if (FilesSilent
							.anyChildExists(dir, "gradlew", "build.gradle")) {
						newGitignoreContent += "\n" + GITIGNORE_ECLIPSE_JAVA;
						newGitignoreContent += "\n" + GITIGNORE_IDEA;
						newGitignoreContent += "\n" + GITIGNORE_GRADLE;
					}
					newGitignoreContent += "\n" + GITIGNORE_SUFFIX;
					updateExistingGitignore(
							dir.resolve(".gitignore"),
							newGitignoreContent
					);
				});
	}

	private void createMavenWrapperIgnore(ChoreContext context) {
		context.textFiles()
				.stream()
				.filter(file -> file.endsWith("mvnw"))
				.map(MyPaths::getParent)
				.forEach(dir -> {
					String newGitignoreContent = GITIGNORE_PREFIX;
					newGitignoreContent += "\n" + GITIGNORE_MAVEN_WRAPPER;
					newGitignoreContent += "\n" + GITIGNORE_SUFFIX;
					updateExistingGitignore(
							dir.resolve(".mvn/wrapper/.gitignore"),
							newGitignoreContent
					);
				});
	}

	private void createGradleWrapperIgnore(ChoreContext context) {
		context.textFiles()
				.stream()
				.filter(file -> file.endsWith("gradlew"))
				.map(MyPaths::getParent)
				.forEach(dir -> {
					String newGitignoreContent = GITIGNORE_PREFIX;
					newGitignoreContent += "\n" + GITIGNORE_GRADLE_WRAPPER;
					newGitignoreContent += "\n" + GITIGNORE_SUFFIX;
					updateExistingGitignore(
							dir.resolve("gradle/wrapper/.gitignore"),
							newGitignoreContent
					);
				});
	}

	private void createEclipseSettingsIgnore(ChoreContext context) {
		Stream.of(
				context.textFiles()
						.stream()
						.filter(file -> file.endsWith("pom.xml"))
						.map(MyPaths::getParent),
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
			String newGitignoreContent = GITIGNORE_PREFIX;
			newGitignoreContent += "\n" + GITIGNORE_ECLIPSE_SETTINGS;
			newGitignoreContent += "\n" + GITIGNORE_SUFFIX;
			updateExistingGitignore(settingsGitignore, newGitignoreContent);
		});
	}

	private void createVscodeSettingsIgnore(ChoreContext context) {
		context.textFiles()
				.stream()
				.map(MyPaths::getParent)
				.filter(file -> file.endsWith(".vscode"))
				.map(MyPaths::getParent)
				.forEach(dir -> {
					Path settingsGitignore = dir.resolve(".vscode/.gitignore");
					String newGitignoreContent = GITIGNORE_PREFIX;
					newGitignoreContent += "\n" + GITIGNORE_VSCODE_SETTINGS;
					newGitignoreContent += "\n" + GITIGNORE_SUFFIX;
					updateExistingGitignore(
							settingsGitignore,
							newGitignoreContent
					);
				});
	}

	private void createIdeaSettingsIgnore(ChoreContext context) {
		context.textFiles()
				.stream()
				.map(MyPaths::getParent)
				.filter(file -> file.endsWith(".idea"))
				.map(MyPaths::getParent)
				.forEach(dir -> {
					Path settingsGitignore = dir.resolve(".idea/.gitignore");
					String newGitignoreContent = GITIGNORE_PREFIX;
					newGitignoreContent += "\n" + GITIGNORE_IDEA_SETTINGS;
					newGitignoreContent += "\n" + GITIGNORE_SUFFIX;
					updateExistingGitignore(
							settingsGitignore,
							newGitignoreContent
					);
				});
	}

}
