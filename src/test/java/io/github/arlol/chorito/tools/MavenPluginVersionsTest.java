package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.chores.EclipseFormatterPluginChore;
import io.github.arlol.chorito.chores.ModernizerPluginChore;
import io.github.arlol.chorito.chores.SpotbugsPluginChore;

/**
 * Renovate bumps the constants in {@link MavenPluginVersions} and nothing else
 * checks that the chores still interpolate them. A chore that went back to a
 * literal would keep compiling right up until the next bump, after which it
 * quietly writes the old version into every managed pom. These assertions turn
 * that into a build failure.
 */
public class MavenPluginVersionsTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private Path pom;

	@BeforeEach
	public void writeEmptyPom() {
		pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pom, """
				<project>
					<build>
						<plugins>
							<plugin>
								<groupId>org.codehaus.mojo</groupId>
								<artifactId>flatten-maven-plugin</artifactId>
							</plugin>
						</plugins>
					</build>
				</project>
				""");
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
	}

	@Test
	public void choresWriteTheDeclaredVersions() {
		new EclipseFormatterPluginChore().doit(extension.choreContext());
		new SpotbugsPluginChore().doit(extension.choreContext());
		new ModernizerPluginChore().doit(extension.choreContext());

		MavenPomFile written = MavenPomFile.read(pom);
		assertThat(written.pluginVersion(MavenPlugins.FORMATTER))
				.contains(MavenPluginVersions.FORMATTER);
		assertThat(written.pluginVersion(MavenPlugins.SPOTBUGS))
				.contains(MavenPluginVersions.SPOTBUGS);
		assertThat(written.pluginVersion(MavenPlugins.MODERNIZER))
				.contains(MavenPluginVersions.MODERNIZER);
	}

}
