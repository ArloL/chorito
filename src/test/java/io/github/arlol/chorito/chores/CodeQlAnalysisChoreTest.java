package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

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

	public String removeVersions(String input) {
		return input.replaceAll("@v[0-9.]+\n", "@\n");
	}

	@Test
	public void testJava() throws Exception {
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		new CodeQlAnalysisChore().doit(context);

		Path workflow = context
				.resolve(".github/workflows/codeql-analysis.yaml");
		assertTrue(FilesSilent.exists(workflow));
		assertThat(removeVersions(FilesSilent.readString(workflow)))
				.isEqualTo(
						removeVersions(
								ClassPathFiles
										.readString("codeql/java-expected.yaml")
						)
				);
	}

	@Test
	public void testAddingPermissions() throws Exception {
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		Path workflow = context
				.resolve(".github/workflows/codeql-analysis.yaml");

		FilesSilent.writeString(
				workflow,
				ClassPathFiles.readString("codeql/input.yaml")
		);

		new CodeQlAnalysisChore().doit(context);

		assertThat(removeVersions(FilesSilent.readString(workflow)))
				.isEqualTo(
						removeVersions(
								ClassPathFiles
										.readString("codeql/expected.yaml")
						)
				);
	}

	@Test
	public void testJavaScript() throws Exception {
		FilesSilent.touch(extension.root().resolve("package.json"));

		ChoreContext context = extension.choreContext()
				.toBuilder()
				.remotes(List.of("https://github.com/example/example"))
				.randomGenerator(new FakeRandomGenerator())
				.build();

		Path workflow = context
				.resolve(".github/workflows/codeql-analysis.yaml");

		new CodeQlAnalysisChore().doit(context);

		assertThat(removeVersions(FilesSilent.readString(workflow))).isEqualTo(
				removeVersions(
						ClassPathFiles
								.readString("codeql/javascript-expected.yaml")
				)
		);
	}

}
