package io.github.arlol.chorito.chores;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class DeleteUnwantedFilesChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new DeleteUnwantedFilesChore().doit(extension.choreContext());
	}

	@Test
	public void test() throws Exception {
		ChoreContext context = context();
		Path dsStore = context.resolve(".DS_Store");
		assertTrue(FilesSilent.exists(dsStore));
		new DeleteUnwantedFilesChore().doit(context());
		assertFalse(FilesSilent.exists(dsStore));
	}

	private ChoreContext context() {
		ChoreContext context = extension.choreContext();
		FilesSilent.writeString(
				context.resolve(".DS_Store"),
				"this is a text file"
		);
		return context;
	}

}
