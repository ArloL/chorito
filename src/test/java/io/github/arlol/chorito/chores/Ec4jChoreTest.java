package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class Ec4jChoreTest {

	private static String DEFAULT_EDITORCONFIG = """
			root = true
			[*]
			end_of_line = lf
			insert_final_newline = true
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void test() throws Exception {
		Path editorConfig = extension.root().resolve(".editorconfig");
		FilesSilent.writeString(editorConfig, DEFAULT_EDITORCONFIG);
		Path text = extension.root().resolve("test.txt");
		FilesSilent.writeString(text, "file with a newline\r\n");

		new Ec4jChore().doit(extension.choreContext());

		assertThat(FilesSilent.readString(text))
				.isEqualTo("file with a newline\n");
	}

}
