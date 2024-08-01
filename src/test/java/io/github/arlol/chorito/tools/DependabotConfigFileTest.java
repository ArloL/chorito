package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DependabotConfigFileTest {

	@Test
	void testEmpty() throws Exception {
		// given
		DependabotConfigFile dependabotConfigFile = new DependabotConfigFile();

		// when

		// then
		assertFalse(
				dependabotConfigFile.hasEcosystemWithDirectory("gradle", "/")
		);
		assertThat(dependabotConfigFile.asString()).isEqualTo("""
				version: 2
				updates: []
				""");
	}

	@Test
	void testCreation() throws Exception {
		// given
		DependabotConfigFile dependabotConfigFile = new DependabotConfigFile();

		// when
		dependabotConfigFile.addEcosystemInDirectory("gradle", "/");

		// then
		assertTrue(
				dependabotConfigFile.hasEcosystemWithDirectory("gradle", "/")
		);
		assertThat(dependabotConfigFile.asString()).isEqualTo("""
				version: 2
				updates:
				- package-ecosystem: "gradle"
				  directory: "/"
				  schedule:
				    interval: "daily"
				""");
	}

	@Test
	void testName() throws Exception {
		String content = """
				version: 2
				updates:
				  - package-ecosystem: "github-actions"
				    directory: "/"
				    schedule:
				      interval: "daily"
				  - package-ecosystem: "docker"
				    directory: "/"
				    schedule:
				      interval: "daily"
				""";
		DependabotConfigFile dependabotConfigFile = new DependabotConfigFile(
				content
		);
		assertTrue(
				dependabotConfigFile
						.hasEcosystemWithDirectory("github-actions", "/")
		);
		assertTrue(
				dependabotConfigFile.hasEcosystemWithDirectory("docker", "/")
		);
		assertFalse(
				dependabotConfigFile.hasEcosystemWithDirectory("gradle", "/")
		);
		dependabotConfigFile.addEcosystemInDirectory("gradle", "/");
		assertTrue(
				dependabotConfigFile.hasEcosystemWithDirectory("gradle", "/")
		);

	}

}
