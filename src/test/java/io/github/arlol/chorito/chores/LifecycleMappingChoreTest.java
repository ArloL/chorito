package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class LifecycleMappingChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new LifecycleMappingChore().doit(extension.choreContext());
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
				ClassPathFiles.readString("lifecycle-mapping/input.xml")
		);

		doit();

		String expected = ClassPathFiles
				.readString("lifecycle-mapping/expected.xml");
		assertThat(pom).content().isEqualTo(expected);
	}

}
