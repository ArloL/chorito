package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MavenPlugins;
import io.github.arlol.chorito.tools.MavenPomFile;
import io.github.arlol.chorito.tools.MavenVersions;

public class EnforcerPluginChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new EnforcerPluginChore().doit(extension.choreContext());
	}

	private Path writePom(String content) {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pom, content);
		return pom;
	}

	@Test
	public void testWithNothing() {
		doit();

		assertThat(extension.relativePaths()).isEmpty();
	}

	@Test
	public void test() throws Exception {
		Path pom = writePom(
				ClassPathFiles.readString("enforcer-plugin/input.xml")
		);

		doit();

		String expected = ClassPathFiles
				.readString("enforcer-plugin/expected.xml");
		assertThat(pom).content().isEqualTo(expected);
	}

	/**
	 * Renovate bumps {@link MavenVersions#MAVEN} and the fixture above
	 * together. A chore that went back to a literal would keep matching the
	 * fixture only until the next bump, after which it quietly writes the old
	 * floor into every managed pom.
	 */
	@Test
	public void writesTheDeclaredVersion() {
		Path pom = writePom(
				ClassPathFiles.readString("enforcer-plugin/input.xml")
		);

		doit();

		assertThat(
				MavenPomFile.read(pom)
						.configuration(
								MavenPlugins.ENFORCER,
								"rules",
								"requireMavenVersion",
								"version"
						)
		).contains(MavenVersions.MAVEN);
	}

	/**
	 * The enforcer block is the repository's decision, so a pom that does not
	 * run the enforcer is not given one.
	 */
	@Test
	public void leavesAPomWithoutTheEnforcerAlone() {
		String content = """
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
				""";
		Path pom = writePom(content);

		doit();

		assertThat(pom).content().isEqualTo(content);
	}

	/**
	 * Nor is a pom that runs the enforcer without the rule: chorito owns the
	 * version, not the decision to enforce one.
	 */
	@Test
	public void leavesAnEnforcerWithoutTheRuleAlone() {
		String content = """
				<project>
					<build>
						<plugins>
							<plugin>
								<groupId>org.apache.maven.plugins</groupId>
								<artifactId>maven-enforcer-plugin</artifactId>
								<configuration>
									<rules>
										<requirePluginVersions />
									</rules>
								</configuration>
							</plugin>
						</plugins>
					</build>
				</project>
				""";
		Path pom = writePom(content);

		doit();

		assertThat(pom).content().isEqualTo(content);
	}

}
