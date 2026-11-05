package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JavaVersions;

public class IdiomaticVersionFileChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new IdiomaticVersionFileChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		doit();

		assertThat(extension.relativePaths()).isEmpty();
	}

	private void setUpProject() {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));
	}

	private void writeToolVersions(String content) {
		FilesSilent.writeString(
				extension.root().resolve(".tool-versions"),
				content
		);
	}

	private void assertToolVersions(String expected) {
		assertThat(extension.root().resolve(".tool-versions")).content()
				.isEqualTo(expected);
	}

	private void assertUnchanged(String input) {
		setUpProject();
		writeToolVersions(input);

		doit();

		assertToolVersions(input);
	}

	@Test
	public void testCreate() throws Exception {
		setUpProject();

		doit();

		assertToolVersions("java temurin-" + JavaVersions.TEMURIN + "\n");
	}

	@Test
	public void testCreateNative() throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.writeString(
				extension.root().resolve("pom.xml"),
				"<project>native-maven-plugin</project>"
		);

		doit();

		assertToolVersions(
				"java graalvm-community-innovation-" + JavaVersions.GRAALVM
						+ "\n"
		);
	}

	@Test
	public void testPinsFloatingTemurin() throws Exception {
		setUpProject();
		writeToolVersions("java temurin-25\n");

		doit();

		assertToolVersions("java temurin-" + JavaVersions.TEMURIN + "\n");
	}

	@Test
	public void testKeepsExactTemurin() throws Exception {
		assertUnchanged("java temurin-25.0.4+7.0.LTS\n");
	}

	@Test
	public void testKeepsOtherTemurinMajor() throws Exception {
		assertUnchanged("java temurin-21\n");
	}

	@Test
	public void testMovesGraalToInnovation() throws Exception {
		setUpProject();
		writeToolVersions("java graalvm-community-25.0.2\n");

		doit();

		assertToolVersions(
				"java graalvm-community-innovation-" + JavaVersions.GRAALVM
						+ "\n"
		);
	}

	@Test
	public void testMovesFloatingGraalToInnovation() throws Exception {
		setUpProject();
		writeToolVersions("java graalvm-community-25\n");

		doit();

		assertToolVersions(
				"java graalvm-community-innovation-" + JavaVersions.GRAALVM
						+ "\n"
		);
	}

	@Test
	public void testKeepsInnovationPin() throws Exception {
		assertUnchanged("java graalvm-community-innovation-25.0.4.1.1\n");
	}

	@Test
	public void testKeepsOtherTools() throws Exception {
		setUpProject();
		writeToolVersions("nodejs 24.22.0\njava temurin-25\n");

		doit();

		assertToolVersions(
				"nodejs 24.22.0\njava temurin-" + JavaVersions.TEMURIN + "\n"
		);
	}

	@Test
	public void testNoJavaLineGetsOne() throws Exception {
		setUpProject();
		writeToolVersions("nodejs 24.22.0\n");

		doit();

		assertToolVersions(
				"nodejs 24.22.0\njava temurin-" + JavaVersions.TEMURIN + "\n"
		);
	}

	@Test
	public void testRootPinnedWhenOnlySubdirectoryIsJava() throws Exception {
		FilesSilent
				.touch(extension.root().resolve("demo/src/main/java/A.java"));
		FilesSilent.touch(extension.root().resolve("demo/pom.xml"));
		writeToolVersions("java temurin-25\n");

		doit();

		assertToolVersions("java temurin-" + JavaVersions.TEMURIN + "\n");
	}

	@Test
	public void testRootNotCreatedWhenOnlySubdirectoryIsJava()
			throws Exception {
		FilesSilent
				.touch(extension.root().resolve("demo/src/main/java/A.java"));
		FilesSilent.touch(extension.root().resolve("demo/pom.xml"));

		doit();

		assertThat(extension.root().resolve(".tool-versions")).doesNotExist();
	}

	@Test
	public void testRootWithoutJavaLineOutsideJavaDirUntouched()
			throws Exception {
		FilesSilent
				.touch(extension.root().resolve("demo/src/main/java/A.java"));
		FilesSilent.touch(extension.root().resolve("demo/pom.xml"));
		writeToolVersions("nodejs 24.22.0\n");

		doit();

		assertToolVersions("nodejs 24.22.0\n");
	}

	@Test
	public void testMultiModuleMavenDoesNotCreateInChildModule()
			throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));
		FilesSilent.writeString(extension.root().resolve("child/pom.xml"), """
				<project>
				<parent>
				<relativePath>..</relativePath>
				</parent>
				</project>
				""");
		FilesSilent.touch(
				extension.root().resolve("child/src/main/java/Child.java")
		);

		doit();

		assertThat(extension.root().resolve(".tool-versions")).exists();
		assertThat(extension.root().resolve("child/.tool-versions"))
				.doesNotExist();
	}

	@Test
	public void testGraal() throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.writeString(
				extension.root().resolve("pom.xml"),
				"""
						<?xml version="1.0" encoding="UTF-8"?>
						<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
							<profiles>
								<profile>
									<id>graal</id>
									<activation>
										<file>
											<exists>${env.JAVA_HOME}/lib/graal</exists>
										</file>
									</activation>
									<build>
										<plugins>
											<plugin>
												<groupId>org.graalvm.buildtools</groupId>
												<artifactId>native-maven-plugin</artifactId>
												<executions>
													<execution>
														<id>build-native</id>
														<goals>
															<goal>compile-no-fork</goal>
														</goals>
														<phase>package</phase>
													</execution>
												</executions>
											</plugin>
										</plugins>
									</build>
								</profile>
							</profiles>
						</project>
						"""
		);

		doit();

		Path toolVersions = extension.root().resolve(".tool-versions");
		assertThat(toolVersions).content()
				.isEqualTo(
						"java graalvm-community-innovation-"
								+ JavaVersions.GRAALVM + "\n"
				);
	}

}
