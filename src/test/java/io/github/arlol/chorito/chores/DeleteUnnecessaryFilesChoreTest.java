package io.github.arlol.chorito.chores;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.PathChoreContext;

public class DeleteUnnecessaryFilesChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void test() throws Exception {
		ChoreContext context = context();
		Path mavenSettings = context
				.resolve(".github/github-packages-maven-settings.xml");
		Path mavenWindowsSettings = context
				.resolve(".github/github-actions-windows-maven-settings.xml");
		assertTrue(FilesSilent.exists(mavenSettings));
		assertTrue(FilesSilent.exists(mavenWindowsSettings));
		new DeleteUnnecessaryFilesChore(context).doit();
		assertFalse(FilesSilent.exists(mavenSettings));
		assertFalse(FilesSilent.exists(mavenWindowsSettings));
	}

	private ChoreContext context() {
		FileSystem fileSystem = this.extension.getFileSystem();
		Path root = fileSystem.getPath("/");
		FilesSilent.writeString(
				root.resolve(".github/github-packages-maven-settings.xml"),
				"this is a text file"
		);
		FilesSilent.writeString(
				root.resolve(
						".github/github-actions-windows-maven-settings.xml"
				),
				"this is a text file"
		);
		return new PathChoreContext(root);
	}

}
