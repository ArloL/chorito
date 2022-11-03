package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class XmlPreambleChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new XmlPreambleChore(extension.choreContext()).doit();
	}

	@Test
	public void test() throws Exception {
		ChoreContext context = extension.choreContext();

		Path pomXml = context.resolve("pom.xml");
		FilesSilent.writeString(pomXml, "");

		new XmlPreambleChore(context.refresh()).doit();

		assertThat(FilesSilent.readString(pomXml))
				.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	}

}
