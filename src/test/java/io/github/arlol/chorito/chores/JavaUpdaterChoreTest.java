package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class JavaUpdaterChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new JavaUpdaterChore(extension.choreContext()).doit();
	}

	@Test
	public void testPomXml() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pom, "<java.version>11</java.version>");

		new JavaUpdaterChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(pom))
				.isEqualTo("<java.version>17</java.version>");
	}

	@Test
	public void testWorkflow() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, "  JAVA_VERSION: 11");

		new JavaUpdaterChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(workflow))
				.isEqualTo("  JAVA_VERSION: 17\n");
	}

	@Test
	public void testJitpackJdk8() throws Exception {
		Path jitpack = extension.root().resolve("jitpack.yml");
		FilesSilent.writeString(jitpack, "- openjdk8");

		new JavaUpdaterChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(jitpack)).isEqualTo("- openjdk17\n");
	}

	@Test
	public void testJitpackJdk11() throws Exception {
		Path jitpack = extension.root().resolve("jitpack.yml");
		FilesSilent.writeString(jitpack, "- openjdk11");

		new JavaUpdaterChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(jitpack)).isEqualTo("- openjdk17\n");
	}

}
