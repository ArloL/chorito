package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FakeRandomGenerator;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class CodeQlAnalysisChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new CodeQlAnalysisChore().doit(extension.choreContext());
	}

	@Test
	public void testJava() throws Exception {
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new CodeQlAnalysisChore().doit(context);

		Path workflow = context
				.resolve(".github/workflows/codeql-analysis.yaml");
		assertTrue(FilesSilent.exists(workflow));
		assertThat(FilesSilent.readString(workflow)).isEqualTo(
				ClassPathFiles.readString("codeql/java-expected.yaml")
		);
	}

	@Test
	public void testAddingPermissions() throws Exception {
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.hasGitHubRemote(true)
				.randomGenerator(new FakeRandomGenerator())
				.build();

		Path workflow = context
				.resolve(".github/workflows/codeql-analysis.yaml");

		FilesSilent.writeString(
				workflow,
				ClassPathFiles.readString("codeql/input.yaml")
		);

		new CodeQlAnalysisChore().doit(context);

		assertThat(FilesSilent.readString(workflow))
				.isEqualTo(ClassPathFiles.readString("codeql/expected.yaml"));
	}

}
