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
			# Created by https://www.toptal.com/developers/gitignore/api/eclipse
			# Edit at https://www.toptal.com/developers/gitignore?templates=eclipse

			### Eclipse ###
			.metadata
			bin/
			tmp/
			*.tmp
			*.bak
			*.swp
			*~.nib
			local.properties
			# .settings/
			.loadpath
			.recommenders

			# External tool builders
			.externalToolBuilders/

			# Locally stored "Eclipse launch configurations"
			*.launch

			# PyDev specific (Python IDE for Eclipse)
			*.pydevproject

			# CDT-specific (C/C++ Development Tooling)
			.cproject

			# CDT- autotools
			.autotools

			# Java annotation processor (APT)
			.factorypath

			# PDT-specific (PHP Development Tools)
			.buildpath

			# sbteclipse plugin
			.target

			# Tern plugin
			.tern-project

			# TeXlipse plugin
			.texlipse

			# STS (Spring Tool Suite)
			.springBeans

			# Code Recommenders
			.recommenders/

			# Annotation Processing
			.apt_generated/
			.apt_generated_test/

			# Scala IDE specific (Scala & Java development for Eclipse)
			.cache-main
			.scala_dependencies
			.worksheet

			# Uncomment this line if you wish to ignore the project description file.
			# Typically, this file would be tracked if it contains build/dependency configurations:
			#.project

			### Eclipse Patch ###
			# Spring Boot Tooling
			.sts4-cache/

			# End of https://www.toptal.com/developers/gitignore/api/eclipse
			""";

	private static String GITIGNORE_MAVEN = """
			# Created by https://www.toptal.com/developers/gitignore/api/maven
			# Edit at https://www.toptal.com/developers/gitignore?templates=maven

			### Maven ###
			target/
			pom.xml.tag
			pom.xml.releaseBackup
			pom.xml.versionsBackup
			pom.xml.next
			release.properties
			dependency-reduced-pom.xml
			buildNumber.properties
			.mvn/timing.properties
			# https://github.com/takari/maven-wrapper#usage-without-binary-jar
			# .mvn/wrapper/maven-wrapper.jar

			# Eclipse m2e generated files
			# Eclipse Core
			.project
			# JDT-specific (Eclipse Java Development Tools)
			.classpath

			# End of https://www.toptal.com/developers/gitignore/api/maven

			.flattened-pom.xml
			""";

	private static String GITIGNORE_SUFFIX = """
			# End of chorito. Add your ignores after this line and they will be preserved.""";

	private static String GITIGNORE_GRADLE = """
			### Gradle ###
			.gradle
			**/build/
			!src/**/build/

			# Ignore Gradle GUI config
			gradle-app.setting

			# Avoid ignoring Gradle wrapper jar file (.jar files are usually ignored)
			!gradle-wrapper.jar

			# Avoid ignore Gradle wrappper properties
			!gradle-wrapper.properties

			# Cache of project
			.gradletasknamecache

			# Eclipse Gradle plugin generated files
			# Eclipse Core
			.project
			# JDT-specific (Eclipse Java Development Tools)
			.classpath

			### Gradle Patch ###
			# Java heap dump
			*.hprof
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
				newGitignoreContent += "\n" + GITIGNORE_MAVEN;
			}
			if (FilesSilent.anyChildExists(dir, "gradlew", "build.gradle")) {
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
