package io.github.arlol.chorito.chores;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FakeProcessBuilderSilent;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.ProcessBuilderSilent;

public class MavenWrapperChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new MavenWrapperChore().doit(extension.choreContext());
	}

	@Test
	void testNada() throws Exception {
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(FakeProcessBuilderSilent.factory())
				.build();
		new MavenWrapperChore().doit(context);
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
