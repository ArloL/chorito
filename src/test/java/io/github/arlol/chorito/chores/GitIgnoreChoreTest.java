package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GitIgnoreChoreTest {

	private static final String DEFAULT_ECLIPSE_SETTINGS = """
			### Eclipse ###

			*
			!/.gitignore
			!/code-formatter-profile.xml
			!/org.eclipse.jdt.core.prefs
			!/org.eclipse.jdt.ui.prefs

			# Add custom ignores after this line to be preserved during automated updates
			""";

	private static String DEFAULT_POM_XML = """
			### Eclipse ###

			/.project

			### Eclipse+Java ###

			/bin/
			/.classpath
			/.factorypath

			### IntelliJ IDEA ###

			/out/

			### Maven ###

			/target/
			/.flattened-pom.xml

			# Add custom ignores after this line to be preserved during automated updates
			""";

	private static String DEFAULT_BUILD_GRADLE = """
			### Eclipse ###

			/.project

			### Eclipse+Java ###

			/bin/
			/.classpath
			/.factorypath

			### IntelliJ IDEA ###

			/out/

			### Gradle ###

			/.gradle/
			/build/

			# Add custom ignores after this line to be preserved during automated updates
			""";

	private static String DEFAULT_IDEA = """
			### IntelliJ IDEA ###

			*
			!/.gitignore
			!/eclipseCodeFormatter.xml
			!/externalDependencies.xml
			!/saveactions_settings.xml
			!/codeStyles
			!/codeStyles/codeStyleConfig.xml
			!/codeStyles/Project.xml

			# Add custom ignores after this line to be preserved during automated updates
			""";

	private static String OLD_POM_XML = """
			# Created by chorito https://github.com/ArloL/chorito

			### Eclipse ###

			/.project

			### Eclipse+Java ###

			/bin/
			/.classpath
			/.factorypath

			### IntelliJ IDEA ###

			/out/

			### Maven ###

			/target/
			/.flattened-pom.xml

			# Add custom ignores after this line to be preserved during automated updates
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
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		doit();

		Path gitignore = extension.root().resolve(".gitignore");
		assertThat(gitignore).content().isEqualTo(DEFAULT_POM_XML);
	}

	@Test
	public void testWithPomAndExistingGitignore() throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));
		Path gitignore = extension.root().resolve(".gitignore");
		FilesSilent.write(gitignore, List.of("lol"), "\n");

		doit();

		assertThat(gitignore).content().isEqualTo(DEFAULT_POM_XML);
	}

	@Test
	public void testWithPomAndExistingGitignoreWithSettings() throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));
		Path gitignore = extension.root().resolve(".gitignore");
		FilesSilent.write(gitignore, List.of(".settings"), "\n");

		doit();

		assertThat(gitignore).content().isEqualTo(DEFAULT_POM_XML);
	}

	@Test
	public void testWithPom() throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		doit();

		Path settingsGitignore = extension.root()
				.resolve(".settings/.gitignore");
		assertThat(settingsGitignore).content()
				.isEqualTo(
						"""
								### Eclipse ###

								*
								!/.gitignore
								!/code-formatter-profile.xml
								!/org.eclipse.jdt.core.prefs
								!/org.eclipse.jdt.ui.prefs

								# Add custom ignores after this line to be preserved during automated updates
								"""
				);
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
								*
								!.gitignore
								!settings.json
								!tasks.json
								!launch.json
								!extensions.json
								!*.code-snippets

								# Add custom ignores after this line to be preserved during automated updates
								"""
				);
	}

	@Test
	public void testWithNestedBuildGradle() throws Exception {
		FilesSilent.touch(
				extension.root().resolve("a/nested/src/main/java/Main.java")
		);
		FilesSilent.writeString(
				extension.root().resolve("a/nested/build.gradle"),
				"java"
		);

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

	@Test
	public void testUpdatingSuffixPrefix() throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));
		FilesSilent.writeString(
				extension.choreContext().resolve(".gitignore"),
				OLD_POM_XML
		);

		doit();

		assertThat(extension.choreContext().resolve(".gitignore")).content()
				.isEqualTo(DEFAULT_POM_XML);
	}

	@Test
	public void testStability() throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.choreContext().resolve("pom.xml"));
		FilesSilent.touch(extension.choreContext().resolve("Dockerfile"));

		doit();
		// call again to see if updates are stable
		doit();

		assertThat(extension.choreContext().resolve(".gitignore")).content()
				.isEqualTo(DEFAULT_POM_XML);
	}

	@Test
	public void testEclipseSettings() throws Exception {
		FilesSilent.touch(
				extension.choreContext()
						.resolve(
								"a/nested/.settings/org.eclipse.buildship.core.prefs"
						)
		);

		doit();

		assertThat(
				extension.choreContext()
						.resolve("a/nested/.settings/.gitignore")
		).content().isEqualTo(DEFAULT_ECLIPSE_SETTINGS);
	}

}
