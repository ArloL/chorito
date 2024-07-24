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
			# Created by https://www.toptal.com/developers/gitignore/api/intellij
			# Edit at https://www.toptal.com/developers/gitignore?templates=intellij

			### Intellij ###
			# Covers JetBrains IDEs: IntelliJ, RubyMine, PhpStorm, AppCode, PyCharm, CLion, Android Studio, WebStorm and Rider
			# Reference: https://intellij-support.jetbrains.com/hc/en-us/articles/206544839

			# User-specific stuff
			**/workspace.xml
			**/tasks.xml
			**/usage.statistics.xml
			**/dictionaries
			**/shelf

			# AWS User-specific
			**/aws.xml

			# Generated files
			**/contentModel.xml

			# Sensitive or high-churn files
			**/dataSources/
			**/dataSources.ids
			**/dataSources.local.xml
			**/sqlDataSources.xml
			**/dynamic.xml
			**/uiDesigner.xml
			**/dbnavigator.xml

			# Gradle
			**/gradle.xml
			**/libraries

			# Gradle and Maven with auto-import
			# When using Gradle or Maven with auto-import, you should exclude module files,
			# since they will be recreated, and may cause churn.  Uncomment if using
			# auto-import.
			artifacts
			compiler.xml
			jarRepositories.xml
			modules.xml
			*.iml
			modules
			# *.iml
			# *.ipr

			# CMake
			# cmake-build-*/

			# Mongo Explorer plugin
			**/mongoSettings.xml

			# File-based project format
			# *.iws

			# IntelliJ
			# out/

			# mpeltonen/sbt-idea plugin
			# .idea_modules/

			# JIRA plugin
			# atlassian-ide-plugin.xml

			# Cursive Clojure plugin
			replstate.xml

			# SonarLint plugin
			sonarlint/

			# Crashlytics plugin (for Android Studio and IntelliJ)
			# com_crashlytics_export_strings.xml
			# crashlytics.properties
			# crashlytics-build.properties
			# fabric.properties

			# Editor-based Rest Client
			httpRequests

			# Android studio 3.1+ serialized cache file
			caches/build_file_checksums.ser

			### Intellij Patch ###
			# Comment Reason: https://github.com/joeblau/gitignore.io/issues/186#issuecomment-215987721

			# *.iml
			# modules.xml
			# misc.xml
			# *.ipr

			# Sonarlint plugin
			# https://plugins.jetbrains.com/plugin/7973-sonarlint
			**/sonarlint/

			# SonarQube Plugin
			# https://plugins.jetbrains.com/plugin/7238-sonarqube-community-plugin
			**/sonarIssues.xml

			# Markdown Navigator plugin
			# https://plugins.jetbrains.com/plugin/7896-markdown-navigator-enhanced
			**/markdown-navigator.xml
			**/markdown-navigator-enh.xml
			**/markdown-navigator/

			# Cache file creation bug
			# See https://youtrack.jetbrains.com/issue/JBR-2257
			$CACHE_FILE$

			# CodeStream plugin
			# https://plugins.jetbrains.com/plugin/12206-codestream
			codestream.xml

			# Azure Toolkit for IntelliJ plugin
			# https://plugins.jetbrains.com/plugin/8053-azure-toolkit-for-intellij
			**/azureSettings.xml

			# End of https://www.toptal.com/developers/gitignore/api/intellij

			encodings.xml
			jarRepositories.xml
			misc.xml
			vcs.xml
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
						newGitignoreContent += "\n" + GITIGNORE_MAVEN;
					}
					if (FilesSilent
							.anyChildExists(dir, "gradlew", "build.gradle")) {
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
