package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class AgentsMarkdownChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new AgentsMarkdownChore().doit(extension.choreContext());

		assertThat(extension.relativePaths()).isEmpty();
	}

	@Test
	public void testLeavesLoneAgentsMarkdownAlone() {
		ChoreContext context = extension.choreContext();
		FilesSilent
				.writeString(context.resolve("AGENTS.md"), "# Instructions\n");

		new AgentsMarkdownChore().doit(context.refresh());

		assertThat(extension.relativePaths()).containsExactly("AGENTS.md");
		assertThat(FilesSilent.readString(context.resolve("AGENTS.md")))
				.isEqualTo("# Instructions\n");
	}

	@Test
	public void testRenamesLoneClaudeMarkdown() {
		ChoreContext context = extension.choreContext();
		FilesSilent
				.writeString(context.resolve("CLAUDE.md"), "# Instructions\n");

		new AgentsMarkdownChore().doit(context.refresh());

		assertThat(extension.relativePaths()).containsExactly("AGENTS.md");
		assertThat(FilesSilent.readString(context.resolve("AGENTS.md")))
				.isEqualTo("# Instructions\n");
	}

	@Test
	public void testDeletesClaudeMarkdownSymlink() throws Exception {
		ChoreContext context = extension.choreContext();
		FilesSilent
				.writeString(context.resolve("AGENTS.md"), "# Instructions\n");
		Files.createSymbolicLink(
				context.resolve("CLAUDE.md"),
				context.root().relativize(context.resolve("AGENTS.md"))
		);

		new AgentsMarkdownChore().doit(context.refresh());

		assertThat(extension.relativePaths()).containsExactly("AGENTS.md");
		assertThat(FilesSilent.readString(context.resolve("AGENTS.md")))
				.isEqualTo("# Instructions\n");
	}

	@Test
	public void testReplacesAgentsMarkdownSymlinkWithItsTargetsContent()
			throws Exception {
		ChoreContext context = extension.choreContext();
		FilesSilent
				.writeString(context.resolve("CLAUDE.md"), "# Instructions\n");
		Files.createSymbolicLink(
				context.resolve("AGENTS.md"),
				context.root().relativize(context.resolve("CLAUDE.md"))
		);

		new AgentsMarkdownChore().doit(context.refresh());

		assertThat(extension.relativePaths()).containsExactly("AGENTS.md");
		assertThat(Files.isSymbolicLink(context.resolve("AGENTS.md")))
				.isFalse();
		assertThat(FilesSilent.readString(context.resolve("AGENTS.md")))
				.isEqualTo("# Instructions\n");
	}

	@Test
	public void testAgentsMarkdownWinsOverRealClaudeMarkdown() {
		ChoreContext context = extension.choreContext();
		FilesSilent.writeString(context.resolve("AGENTS.md"), "# Agents\n");
		FilesSilent.writeString(context.resolve("CLAUDE.md"), "# Claude\n");

		new AgentsMarkdownChore().doit(context.refresh());

		assertThat(extension.relativePaths()).containsExactly("AGENTS.md");
		assertThat(FilesSilent.readString(context.resolve("AGENTS.md")))
				.isEqualTo("# Agents\n");
	}

	@Test
	public void testMaterializesClaudeMarkdownSymlinkPointingElsewhere()
			throws Exception {
		ChoreContext context = extension.choreContext();
		FilesSilent.writeString(
				context.resolve("docs/instructions.md"),
				"# Instructions\n"
		);
		Files.createSymbolicLink(
				context.resolve("CLAUDE.md"),
				context.root()
						.relativize(context.resolve("docs/instructions.md"))
		);

		new AgentsMarkdownChore().doit(context.refresh());

		assertThat(extension.relativePaths()).contains("AGENTS.md");
		assertThat(Files.isSymbolicLink(context.resolve("CLAUDE.md")))
				.isFalse();
		assertThat(FilesSilent.exists(context.resolve("CLAUDE.md"))).isFalse();
		assertThat(FilesSilent.readString(context.resolve("AGENTS.md")))
				.isEqualTo("# Instructions\n");
	}

	@Test
	public void testRenamesClaudeMarkdownInNestedDirectories() {
		ChoreContext context = extension.choreContext();
		FilesSilent.writeString(
				context.resolve("modules/api/CLAUDE.md"),
				"# Api\n"
		);

		new AgentsMarkdownChore().doit(context.refresh());

		assertThat(extension.relativePaths()).contains("modules/api/AGENTS.md")
				.doesNotContain("modules/api/CLAUDE.md");
		assertThat(
				FilesSilent.readString(context.resolve("modules/api/AGENTS.md"))
		).isEqualTo("# Api\n");
	}

	@Test
	public void testDeletesBrokenClaudeMarkdownSymlink() throws Exception {
		ChoreContext context = extension.choreContext();
		FilesSilent.writeString(context.resolve("README.md"), "# Readme\n");
		Files.createSymbolicLink(
				context.resolve("CLAUDE.md"),
				context.root().relativize(context.resolve("nowhere.md"))
		);

		new AgentsMarkdownChore().doit(context.refresh());

		assertThat(extension.relativePaths()).containsExactly("README.md");
	}

}
