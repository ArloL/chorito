package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class IntellijChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new IntellijChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		doit();
	}

	@Test
	public void test() throws Exception {
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		doit();

		Path codeStyleConfig = extension.root()
				.resolve(".idea/codeStyles/codeStyleConfig.xml");

		assertThat(codeStyleConfig).exists();
		assertThat(codeStyleConfig).isNotEmptyFile();
	}

}
