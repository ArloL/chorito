package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GraalNativeProfileSkipChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private static final String PLUGIN = """
						<build>
							<plugins>
								<plugin>
									<groupId>org.graalvm.buildtools</groupId>
									<artifactId>native-maven-plugin</artifactId>
								</plugin>
							</plugins>
						</build>
			""";

	private static final String EXACT_ACTIVATION = """
						<activation>
							<file>
								<exists>${env.JAVA_HOME}/lib/graal</exists>
							</file>
							<property>
								<name>!env.SKIP_GRAALVM_NATIVE_IMAGE_BUILD</name>
							</property>
						</activation>
			""";

	private static String pom(String activation, boolean withPlugin) {
		return """
				<project>
					<profiles>
						<profile>
							<id>graal</id>
				%s%s			</profile>
					</profiles>
				</project>
				""".formatted(activation, withPlugin ? PLUGIN : "");
	}

	private static String activationWithFile(String exists) {
		return """
							<activation>
								<file>
									<exists>%s</exists>
								</file>
							</activation>
				""".formatted(exists);
	}

	private static final String EXPECTED = pom(EXACT_ACTIVATION, true);

	private void doit() {
		new GraalNativeProfileSkipChore().doit(extension.choreContext());
	}

	private Path write(String file, String content) {
		Path pom = extension.root().resolve(file);
		FilesSilent.writeString(pom, content);
		return pom;
	}

	private void assertBecomesExactAndStaysSo(String before) {
		Path pom = write("pom.xml", before);

		doit();

		assertThat(pom).content().isEqualTo(EXPECTED);
		doit();
		assertThat(pom).content().isEqualTo(EXPECTED);
	}

	private void assertUntouched(String content) {
		Path pom = write("pom.xml", content);

		doit();

		assertThat(pom).content().isEqualTo(content);
	}

	@Test
	public void testLibGraalWithoutPropertyGainsProperty() {
		assertBecomesExactAndStaysSo(
				pom(activationWithFile("${env.JAVA_HOME}/lib/graal"), true)
		);
	}

	@Test
	public void testReadmeBecomesExactActivation() {
		assertBecomesExactAndStaysSo(
				pom(
						activationWithFile(
								"${env.JAVA_HOME}/GRAALVM-README.md"
						),
						true
				)
		);
	}

	@Test
	public void testUnknownExistsBecomesExactActivation() {
		assertBecomesExactAndStaysSo(
				pom(
						activationWithFile(
								"${env.GRAALVM_HOME}/bin/native-image"
						),
						true
				)
		);
	}

	@Test
	public void testJdkActivationBecomesExactActivation() {
		assertBecomesExactAndStaysSo(pom("""
							<activation>
								<jdk>[21,)</jdk>
							</activation>
				""", true));
	}

	@Test
	public void testPropertyOnlyActivationBecomesExactActivation() {
		assertBecomesExactAndStaysSo(pom("""
							<activation>
								<property>
									<name>native</name>
								</property>
							</activation>
				""", true));
	}

	@Test
	public void testMissingActivationIsAddedAfterId() {
		assertBecomesExactAndStaysSo(pom("", true));
	}

	@Test
	public void testAlreadyExactIsNotWritten() throws Exception {
		Path pom = write("pom.xml", EXPECTED);
		FileTime old = FileTime.fromMillis(1_000_000_000L);
		Files.setLastModifiedTime(pom, old);

		doit();

		assertThat(pom).content().isEqualTo(EXPECTED);
		assertThat(Files.getLastModifiedTime(pom)).isEqualTo(old);
	}

	@Test
	public void testExactWithOtherFormattingIsNotRewritten() {
		String content = EXPECTED.replace(
				"<exists>${env.JAVA_HOME}/lib/graal</exists>",
				"<exists>  ${env.JAVA_HOME}/lib/graal  </exists>"
		);
		assertThat(content).isNotEqualTo(EXPECTED);
		assertUntouched(content);
	}

	@Test
	public void testProfileWithoutPluginIsUntouched() {
		assertUntouched(
				pom(activationWithFile("${env.JAVA_HOME}/lib/graal"), false)
		);
	}

	@Test
	public void testProfileWithoutPluginAndReadmeIsUntouched() {
		assertUntouched(
				pom(
						activationWithFile(
								"${env.JAVA_HOME}/GRAALVM-README.md"
						),
						false
				)
		);
	}

	@Test
	public void testProfileWithoutPluginAndActivationIsUntouched() {
		assertUntouched(pom("", false));
	}

	@Test
	public void testPluginOutsideProfileIsUntouched() {
		assertUntouched("""
				<project>
					<build>
						<plugins>
							<plugin>
								<groupId>org.graalvm.buildtools</groupId>
								<artifactId>native-maven-plugin</artifactId>
							</plugin>
						</plugins>
					</build>
				</project>
				""");
	}

	@Test
	public void testIgnoresPomWithoutProfile() {
		assertUntouched(
				"<project>\n\t<modelVersion>4.0.0</modelVersion>\n</project>\n"
		);
	}

	@Test
	public void testHandlesChildModule() {
		String before = pom(
				activationWithFile("${env.JAVA_HOME}/GRAALVM-README.md"),
				true
		);
		Path parent = write("pom.xml", before);
		Path child = write("child/pom.xml", before);

		doit();

		assertThat(parent).content().isEqualTo(EXPECTED);
		assertThat(child).content().isEqualTo(EXPECTED);
	}

}
