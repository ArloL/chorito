package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class PomPropertiesChoreTest {

	private static String INPUT = """
			<project>
			<parent>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-parent</artifactId>
			<version>3.0.0</version>
			<relativePath />
			</parent>
			<properties>
			<mainClass>io.github.something.Class</mainClass>
			<other>${mainClass}</other>
			</properties>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-jar-plugin</artifactId>
				<configuration>
					<archive>
						<manifest>
							<mainClass>${start-class}</mainClass>
						</manifest>
					</archive>
				</configuration>
			</plugin>
			</project>
			""";
	private static String EXPECTED = """
			<project>
			<parent>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-parent</artifactId>
			<version>3.0.0</version>
			<relativePath />
			</parent>
			<properties>
			<start-class>io.github.something.Class</start-class>
			<other>${start-class}</other>
			</properties>

			</project>
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new PomPropertiesChore(extension.choreContext()).doit();
	}

	@Test
	public void testWithPom() {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pom, INPUT);

		new PomPropertiesChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(pom)).isEqualTo(EXPECTED);
	}

}
