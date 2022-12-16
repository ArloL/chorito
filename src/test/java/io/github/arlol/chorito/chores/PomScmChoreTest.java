package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class PomScmChoreTest {

	private static String INPUT = """
			<project>
			<scm>
			<connection>scm:git:https://example.com/wrong/wrong.git</connection>
			<developerConnection>scm:git:https://example.com/wrong/wrong.git</developerConnection>
			<url>https://example.com/wrong/wrong/</url>
			</scm>
			</project>
			""";
	private static String EXPECTED = """
			<project>
			<scm>
			<connection>scm:git:https://github.com/ArloL/chorito.git</connection>
			<developerConnection>scm:git:https://github.com/ArloL/chorito.git</developerConnection>
			<url>https://github.com/ArloL/chorito/</url>
			</scm>
			</project>
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new PomScmChore(extension.choreContext()).doit();
	}

	@Test
	public void testWithPom() {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pom, INPUT);

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/ArloL/chorito.git"))
				.hasGitHubRemote(true)
				.build();

		new PomScmChore(context).doit();

		assertThat(FilesSilent.readString(pom)).isEqualTo(EXPECTED);
	}

}
