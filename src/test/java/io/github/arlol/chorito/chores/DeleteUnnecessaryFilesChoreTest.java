package io.github.arlol.chorito.chores;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class DeleteUnnecessaryFilesChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new DeleteUnnecessaryFilesChore().doit(extension.choreContext());
	}

	@Test
	public void test() throws Exception {
		Path mavenSettings = extension.root()
				.resolve(".github/github-packages-maven-settings.xml");
		Path mavenWindowsSettings = extension.root()
				.resolve(".github/github-actions-windows-maven-settings.xml");
		FilesSilent.writeString(mavenSettings, "this is a text file");
		FilesSilent.writeString(mavenWindowsSettings, "this is a text file");

		new DeleteUnnecessaryFilesChore().doit(extension.choreContext());

		assertFalse(FilesSilent.exists(mavenSettings));
		assertFalse(FilesSilent.exists(mavenWindowsSettings));
	}

}
