package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GraalNativeProfileSkipChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private static final String PROFILE = """
			<project>
				<profiles>
					<profile>
						<id>graal</id>
						<activation>
							<file>
								<exists>${env.JAVA_HOME}/lib/graal</exists>
							</file>
						</activation>
					</profile>
				</profiles>
			</project>
			""";

	private static final String EXPECTED = """
			<project>
				<profiles>
					<profile>
						<id>graal</id>
						<activation>
							<file>
								<exists>${env.JAVA_HOME}/lib/graal</exists>
							</file>
							<property>
								<name>!env.SKIP_GRAALVM_NATIVE_IMAGE_BUILD</name>
							</property>
						</activation>
					</profile>
				</profiles>
			</project>
			""";

	private void doit() {
		new GraalNativeProfileSkipChore().doit(extension.choreContext());
	}

	private Path write(String file, String content) {
		Path pom = extension.root().resolve(file);
		FilesSilent.writeString(pom, content);
		return pom;
	}

	@Test
	public void testAddsPropertyToGraalProfile() {
		Path pom = write("pom.xml", PROFILE);

		doit();

		assertThat(pom).content().isEqualTo(EXPECTED);
	}

	@Test
	public void testHandlesChildModule() {
		Path parent = write("pom.xml", PROFILE);
		Path child = write("child/pom.xml", PROFILE);

		doit();

		assertThat(parent).content().isEqualTo(EXPECTED);
		assertThat(child).content().isEqualTo(EXPECTED);
	}

	@Test
	public void testIgnoresPomWithoutProfile() {
		String content = "<project>\n\t<modelVersion>4.0.0</modelVersion>\n</project>\n";
		Path pom = write("pom.xml", content);

		doit();

		assertThat(pom).content().isEqualTo(content);
	}

	@Test
	public void testIgnoresOtherActivation() {
		String content = PROFILE.replaceAll(
				"(?s)<file>.*</file>",
				"<activeByDefault>true</activeByDefault>"
		);
		assertThat(content).isNotEqualTo(PROFILE);
		Path pom = write("pom.xml", content);

		doit();

		assertThat(pom).content().isEqualTo(content);
	}

	@Test
	public void testIsIdempotent() {
		Path pom = write("pom.xml", PROFILE);
		doit();

		doit();

		assertThat(pom).content().isEqualTo(EXPECTED);
	}

}
