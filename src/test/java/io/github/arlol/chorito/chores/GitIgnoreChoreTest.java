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

			### Eclipse ###

			/.project

			### Eclipse+Java ###

			/.classpath

			### Maven ###

			/target/
			/.flattened-pom.xml

			# End of chorito. Add your ignores after this line and they will be preserved.
			""";

	private static String DEFAULT_BUILD_GRADLE = """
			# Created by chorito https://github.com/ArloL/chorito

			### Eclipse ###

			/.project

			### Eclipse+Java ###

			/.classpath

			### Gradle ###

			/.gradle/
			/build/

			# End of chorito. Add your ignores after this line and they will be preserved.
			""";

	private static String DEFAULT_IDEA = """
			# Created by chorito https://github.com/ArloL/chorito

			*
			!/.gitignore
			!/eclipseCodeFormatter.xml
			!/externalDependencies.xml
			!/saveactions_settings.xml
			!/codeStyles
			!/codeStyles/codeStyleConfig.xml
			!/codeStyles/Project.xml

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
