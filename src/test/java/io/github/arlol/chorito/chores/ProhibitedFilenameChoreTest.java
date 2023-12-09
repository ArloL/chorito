package io.github.arlol.chorito.chores;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class ProhibitedFilenameChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new ProhibitedFilenameChore().doit(extension.choreContext());
	}

	@Test
	public void test() throws Exception {
		ChoreContext context = context();
		Path nul = context.resolve("NUL");
		Path nulTxt = context.resolve("NUL.txt");
		assertTrue(FilesSilent.exists(nul));
		assertTrue(FilesSilent.exists(nulTxt));
		new ProhibitedFilenameChore().doit(context());
		assertFalse(FilesSilent.exists(nul));
		assertFalse(FilesSilent.exists(nulTxt));
	}

	private ChoreContext context() {
		ChoreContext context = extension.choreContext();
		FilesSilent.writeString(context.resolve("NUL"), "this is a text file");
		FilesSilent
				.writeString(context.resolve("NUL.txt"), "this is a text file");
		return context;
	}

}
