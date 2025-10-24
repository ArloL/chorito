package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class IdiomaticVersionFileChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new IdiomaticVersionFileChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		doit();
	}

	@Test
	public void testCreate() throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		doit();

		Path toolVersions = extension.root().resolve(".tool-versions");
		assertThat(toolVersions).content().isEqualTo("""
				java temurin-25
				""");
	}

	@Test
	public void testNoOverwrite() throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));
		FilesSilent.touch(extension.root().resolve(".tool-versions"));

		doit();

		Path toolVersions = extension.root().resolve(".tool-versions");
		assertThat(toolVersions).content().isEqualTo("""
				""");
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
		assertThat(toolVersions).content().isEqualTo("""
				java graalvm-community-25
				""");
	}

}
