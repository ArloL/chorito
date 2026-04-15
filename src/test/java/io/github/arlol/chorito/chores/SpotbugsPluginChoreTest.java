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
		new SpotbugsPluginChore().doit(extension.choreContext());
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
				ClassPathFiles.readString("spotbugs-plugin/input.xml")
		);
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));

		doit();

		String expected = ClassPathFiles
				.readString("spotbugs-plugin/expected.xml");
		assertThat(pom).content().isEqualTo(expected);
	}

	@Test
	public void testMultiModuleChildIsSkipped() throws Exception {
		Path rootPom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(
				rootPom,
				ClassPathFiles.readString("spotbugs-plugin/input.xml")
		);

		Path childPom = extension.root().resolve("module/pom.xml");
		String childContent = """
				<project>
					<parent>
						<groupId>io.github.arlol</groupId>
						<artifactId>parent</artifactId>
						<version>1.0</version>
						<relativePath>../pom.xml</relativePath>
					</parent>
				</project>
				""";
		FilesSilent.writeString(childPom, childContent);
		FilesSilent.touch(
				extension.root().resolve("module/src/main/java/Main.java")
		);

		doit();

		assertThat(childPom).content().isEqualTo(childContent);
	}

}
