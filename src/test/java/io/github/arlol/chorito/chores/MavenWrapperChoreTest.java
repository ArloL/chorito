package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FakeProcessBuilderSilent;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MavenVersions;
import io.github.arlol.chorito.tools.ProcessBuilderSilent;

public class MavenWrapperChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new MavenWrapperChore().doit(extension.choreContext());

		assertThat(extension.relativePaths()).isEmpty();
	}

	@Test
	void testNada() throws Exception {
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(FakeProcessBuilderSilent.factory())
				.build();
		new MavenWrapperChore().doit(context);

		assertThat(extension.relativePaths()).isEmpty();
	}

	@Test
	void testCreateMavenWrapper() throws Exception {
		// given
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(
						FakeProcessBuilderSilent.factory(this::fakeMavenWrapper)
				)
				.build();
		Path pom = context.resolve("pom.xml");
		FilesSilent.touch(pom);

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertTrue(FilesSilent.exists(context.resolve("mvnw")));
	}

	@Test
	void testNestedMavenProjects() throws Exception {
		// given
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(
						FakeProcessBuilderSilent.factory(this::fakeMavenWrapper)
				)
				.build();
		FilesSilent.touch(context.resolve("pom.xml"));
		FilesSilent.writeString(context.resolve("nested/pom.xml"), """
				<project>
				<parent>
				<relativePath>..</relativePath>
				</parent>
				</project>
				""");

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertTrue(FilesSilent.exists(context.resolve("mvnw")));
		assertFalse(FilesSilent.exists(context.resolve("nested/mvnw")));
	}

	/**
	 * Renovate bumps {@link MavenVersions#MAVEN} and nothing else checks that
	 * this chore still interpolates it. A wrapper left on the old distribution
	 * installs a Maven older than the floor {@link EnforcerPluginChore} writes
	 * into the pom, which fails every build in the repository rather than
	 * quietly doing nothing.
	 */
	@Test
	void runsTheWrapperForTheDeclaredMavenVersion() throws Exception {
		// given
		List<String> commands = new ArrayList<>();
		var context = recordingContext(commands);
		FilesSilent.touch(context.resolve("pom.xml"));

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertThat(commands).isNotEmpty()
				.allSatisfy(
						command -> assertThat(command)
								.contains("-Dmaven=" + MavenVersions.MAVEN)
				);
	}

	/**
	 * And the properties the chore compares against name the same version, so a
	 * wrapper already on it is left as it is instead of being rewritten on
	 * every run.
	 */
	@Test
	void leavesAWrapperOnTheDeclaredMavenVersionAlone() throws Exception {
		// given
		List<String> commands = new ArrayList<>();
		var context = recordingContext(commands);
		FilesSilent.touch(context.resolve("pom.xml"));
		FilesSilent.touch(context.resolve("mvnw"));
		FilesSilent.touch(context.resolve(".mvn/wrapper/maven-wrapper.jar"));
		FilesSilent.writeString(
				context.resolve(".mvn/wrapper/maven-wrapper.properties"),
				"""
						wrapperVersion=3.3.4
						distributionType=bin
						distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${maven}/apache-maven-${maven}-bin.zip
						wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar
						"""
						.replace("${maven}", MavenVersions.MAVEN)
		);

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertThat(commands).isEmpty();
	}

	/**
	 * A context whose fake wrapper records the command line it was asked to
	 * run, so a test can assert on the version the chore passes rather than
	 * only on the files it leaves behind.
	 */
	private ChoreContext recordingContext(List<String> commands) {
		return extension.choreContext()
				.toBuilder()
				.processBuilderFactory(command -> {
					commands.add(String.join(" ", command));
					return new FakeProcessBuilderSilent(
							command,
							this::fakeMavenWrapper
					);
				})
				.build();
	}

	private void fakeMavenWrapper(ProcessBuilderSilent processBuilderSilent) {
		Path directory = processBuilderSilent.directory();
		FilesSilent.touch(directory.resolve("mvnw"));
		FilesSilent.touch(directory.resolve("mvnw.cmd"));
		FilesSilent.touch(directory.resolve(".mvn/wrapper/maven-wrapper.jar"));
		FilesSilent.touch(
				directory.resolve(".mvn/wrapper/maven-wrapper.properties")
		);
	}

}
