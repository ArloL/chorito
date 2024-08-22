package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class PomParentRelativePathChoreTest {

	private static String INPUT = """
			<project>
			  <parent>
			    <groupId>group</groupId>
			    <artifactId>artifactId</artifactId>
			    <version>1</version>
			  </parent>
			</project>
			""";
	private static String EXPECTED = """
			<project>
			  <parent>
			    <groupId>group</groupId>
			    <artifactId>artifactId</artifactId>
			    <version>1</version>
			  <relativePath />
			</parent>
			</project>
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new PomParentRelativePathChore().doit(extension.choreContext());
	}

	@Test
	public void testWithPom() {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pom, INPUT);

		new PomParentRelativePathChore().doit(extension.choreContext());

		assertThat(pom).content().isEqualTo(EXPECTED);
	}

}
