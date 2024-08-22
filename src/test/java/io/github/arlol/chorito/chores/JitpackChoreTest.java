package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class JitpackChoreTest {

	private static final String EXPECT_JITPACK = """
			jdk:
			- openjdk21
			install:
			- ./mvnw --batch-mode install -DskipTests -Dsha1="${GIT_COMMIT}" -Drevision="${VERSION}"
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		Path jitpack = extension.root().resolve("jitpack.yml");

		new JitpackChore().doit(extension.choreContext());

		assertFalse(FilesSilent.exists(jitpack));
	}

	@Test
	public void test() throws Exception {
		Path jitpack = extension.root().resolve("jitpack.yml");
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pom, "<project />");

		new JitpackChore().doit(extension.choreContext());

		assertTrue(FilesSilent.exists(jitpack));
		assertThat(jitpack).content().isEqualTo(EXPECT_JITPACK);
	}

}
