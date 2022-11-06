package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FakeSilentProcessBuilder;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class MavenWrapperChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new MavenWrapperChore(extension.choreContext()).doit();
	}

	@Test
	void testNada() throws Exception {
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(FakeSilentProcessBuilder.factory())
				.build();
		new MavenWrapperChore(context).doit();
	}

	@Test
	void testCreateMavenWrapper() throws Exception {
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(
						FakeSilentProcessBuilder.factory(this::fakeMavenWrapper)
				)
				.build();
		Path pom = context.resolve("pom.xml");
		FilesSilent.touch(pom);

		new MavenWrapperChore(context).doit();
	}

	private void fakeMavenWrapper() {
		var context = extension.choreContext();
		FilesSilent.touch(context.resolve("mvnw"));
		FilesSilent.touch(context.resolve("mvnw.cmd"));
		FilesSilent.touch(context.resolve(".mvn/wrapper/maven-wrapper.jar"));
		FilesSilent.touch(
				context.resolve(".mvn/wrapper/maven-wrapper.properties")
		);
	}

}
