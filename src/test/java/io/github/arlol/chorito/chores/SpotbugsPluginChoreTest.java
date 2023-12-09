package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class SpotbugsPluginChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new SpotbugsPluginChore(extension.choreContext()).doit();
	}

	@Test
	public void testWithNothing() {
		doit();
	}

	@Test
	public void test() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(
				pom,
				ClassPathFiles.readString("/spotbugs-plugin/input.xml")
		);

		doit();

		String expected = ClassPathFiles
				.readString("/spotbugs-plugin/expected.xml");
		assertThat(FilesSilent.readString(pom)).isEqualTo(expected);
	}

}
