package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.ExistingFileUpdater;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class GitIgnoreChore implements Chore {

	private static String GITIGNORE_ECLIPSE = """
			### Eclipse ###

			/.project
			""";

	private static String GITIGNORE_ECLIPSE_JAVA = """
			### Eclipse+Java ###

			/bin/
			/.classpath
			/.factorypath
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
			### Eclipse ###

			*
			!/.gitignore
			!/code-formatter-profile.xml
			!/org.eclipse.jdt.core.prefs
			!/org.eclipse.jdt.ui.prefs
			""";

	private static String GITIGNORE_IDEA_SETTINGS = """
			### IntelliJ IDEA ###

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

	private static String GITIGNORE_PACKAGE_JSON = """
			/node_modules/
			""";

	private static String GITIGNORE_YARN = """
			*
			!.gitignore
			!/releases/
			!/releases/**/*
			!/patches/
			!/patches/**/*
			!/plugins/
			!/plugins/**/*
			!/sdks/
			!/sdks/**/*
			!/versions/
			!/versions/**/*
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
		createPackageJsonIgnore(context);
		createYarnIgnore(context);
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
		ExistingFileUpdater.update(gitignore, newGitignoreContent);
	}

	private void createPackageJsonIgnore(ChoreContext context) {
		DirectoryStreams.packageJsonDirs(context).forEach(dir -> {
			String newGitignoreContent = GITIGNORE_PACKAGE_JSON;
			updateExistingGitignore(
					dir.resolve(".gitignore"),
					newGitignoreContent
			);
		});
	}

	private void createYarnIgnore(ChoreContext context) {
		DirectoryStreams.dotYarnDirs(context).forEach(dir -> {
			String newGitignoreContent = GITIGNORE_YARN;
			updateExistingGitignore(
					dir.resolve(".gitignore"),
					newGitignoreContent
			);
		});
	}

	private void createMavenAndGradleIgnore(ChoreContext context) {
		DirectoryStreams.javaDirs(context).forEach(dir -> {
			String newGitignoreContent = GITIGNORE_ECLIPSE;
			if (FilesSilent.anyChildExists(dir, "mvnw", "pom.xml")) {
				newGitignoreContent += "\n" + GITIGNORE_ECLIPSE_JAVA;
				newGitignoreContent += "\n" + GITIGNORE_IDEA;
				newGitignoreContent += "\n" + GITIGNORE_MAVEN;
			}
			if (FilesSilent.anyChildExists(dir, "gradlew", "build.gradle")) {
				newGitignoreContent += "\n" + GITIGNORE_ECLIPSE_JAVA;
				newGitignoreContent += "\n" + GITIGNORE_IDEA;
				newGitignoreContent += "\n" + GITIGNORE_GRADLE;
			}
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
					updateExistingGitignore(
							dir.resolve(".mvn/wrapper/.gitignore"),
							GITIGNORE_MAVEN_WRAPPER
					);
				});
	}

	private void createGradleWrapperIgnore(ChoreContext context) {
		context.textFiles()
				.stream()
				.filter(file -> file.endsWith("gradlew"))
				.map(MyPaths::getParent)
				.forEach(dir -> {
					updateExistingGitignore(
							dir.resolve("gradle/wrapper/.gitignore"),
							GITIGNORE_GRADLE_WRAPPER
					);
				});
	}

	private void createEclipseSettingsIgnore(ChoreContext context) {
		Stream.of(
				DirectoryStreams.javaDirs(context),
				context.textFiles()
						.stream()
						.filter(
								file -> MyPaths.getFileNameAsString(file)
										.startsWith("org.eclipse.")
						)
						.map(MyPaths::getParent)
						.filter(file -> file.endsWith(".settings"))
						.map(MyPaths::getParent)
		).flatMap(Function.identity()).distinct().forEach(dir -> {
			updateExistingGitignore(
					dir.resolve(".settings/.gitignore"),
					GITIGNORE_ECLIPSE_SETTINGS
			);
		});
	}

	private void createVscodeSettingsIgnore(ChoreContext context) {
		context.textFiles()
				.stream()
				.map(MyPaths::getParent)
				.filter(file -> file.endsWith(".vscode"))
				.map(MyPaths::getParent)
				.forEach(dir -> {
					updateExistingGitignore(
							dir.resolve(".vscode/.gitignore"),
							GITIGNORE_VSCODE_SETTINGS
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
					updateExistingGitignore(
							dir.resolve(".idea/.gitignore"),
							GITIGNORE_IDEA_SETTINGS
					);
				});
	}

}
