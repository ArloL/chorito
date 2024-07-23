package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitIgnoreChoreTest {

	private static String DEFAULT_POM_XML = """
			# Created by chorito https://github.com/ArloL/chorito

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

			# End of chorito. Add your ignores after this line and they will be preserved.
			""";

	private static String DEFAULT_BUILD_GRADLE = """
			# Created by chorito https://github.com/ArloL/chorito

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

			# End of chorito. Add your ignores after this line and they will be preserved.
			""";

	private static String DEFAULT_IDEA = """
			# Created by chorito https://github.com/ArloL/chorito

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

			# End of chorito. Add your ignores after this line and they will be preserved.
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new GitIgnoreChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		Path gitignore = extension.root().resolve(".gitignore");
		doit();
		assertThat(FilesSilent.exists(gitignore)).isFalse();
	}

	@Test
	public void testWithPomAndNoGitignore() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);

		doit();

		Path gitignore = extension.root().resolve(".gitignore");
		assertThat(FilesSilent.readString(gitignore))
				.isEqualTo(DEFAULT_POM_XML);
	}

	@Test
	public void testWithPomAndExistingGitignore() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);
		Path gitignore = extension.root().resolve(".gitignore");
		FilesSilent.write(gitignore, List.of("lol"), "\n");

		doit();

		assertThat(FilesSilent.readString(gitignore))
				.isEqualTo(DEFAULT_POM_XML);
	}

	@Test
	public void testWithPomAndExistingGitignoreWithSettings() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);
		Path gitignore = extension.root().resolve(".gitignore");
		FilesSilent.write(gitignore, List.of(".settings"), "\n");

		doit();

		assertThat(FilesSilent.readString(gitignore))
				.isEqualTo(DEFAULT_POM_XML);
	}

	@Test
	public void testWithPom() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);

		doit();

		Path settingsGitignore = extension.root()
				.resolve(".settings/.gitignore");
		assertThat(settingsGitignore).content()
				.isEqualTo(
						"""
								# Created by chorito https://github.com/ArloL/chorito

								*.prefs
								!org.eclipse.jdt.core.prefs
								!org.eclipse.jdt.ui.prefs

								# End of chorito. Add your ignores after this line and they will be preserved.
								"""
				);
	}

	@Test
	public void testWithNestedMvnw() throws Exception {
		FilesSilent.touch(extension.root().resolve("a/nested/mvnw"));

		doit();

		assertThat(extension.root().resolve("a/nested/.gitignore")).content()
				.isEqualTo(DEFAULT_POM_XML);
	}

	@Test
	public void testWithNestedVscode() throws Exception {
		FilesSilent.touch(
				extension.root().resolve("a/nested/.vscode/settings.json")
		);

		doit();

		assertThat(extension.root().resolve("a/nested/.vscode/.gitignore"))
				.content()
				.isEqualTo(
						"""
								# Created by chorito https://github.com/ArloL/chorito

								*
								!.gitignore
								!settings.json
								!tasks.json
								!launch.json
								!extensions.json
								!*.code-snippets

								# End of chorito. Add your ignores after this line and they will be preserved.
								"""
				);
	}

	@Test
	public void testWithNestedBuildGradle() throws Exception {
		FilesSilent.touch(extension.root().resolve("a/nested/build.gradle"));

		doit();

		assertThat(extension.root().resolve("a/nested/.gitignore")).content()
				.isEqualTo(DEFAULT_BUILD_GRADLE);
	}

	@Test
	public void testWithNestedIdea() throws Exception {
		FilesSilent.touch(
				extension.root()
						.resolve("a/nested/.idea/externalDependencies.xml")
		);

		doit();

		assertThat(extension.root().resolve("a/nested/.idea/.gitignore"))
				.content()
				.isEqualTo(DEFAULT_IDEA);
	}

}
