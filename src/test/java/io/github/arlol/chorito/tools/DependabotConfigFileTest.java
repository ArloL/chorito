package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
				    interval: "monthly"
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

	@Test
	void testAddCooldownIfMissing() throws Exception {
		var dependabotConfigFile = new DependabotConfigFile("""
				updates:
				- package-ecosystem: "github-actions"
				""");
		dependabotConfigFile.addCooldownIfMissing();
		assertEquals("""
				updates:
				- package-ecosystem: "github-actions"
				  cooldown:
				    default-days: 7
				""", dependabotConfigFile.asString());
	}

	@Test
	void testAddGitHubCodeQlActionGroupIfMissing() throws Exception {
		var dependabotConfigFile = new DependabotConfigFile("""
				updates:
				- package-ecosystem: "github-actions"
				- package-ecosystem: "maven"
				""");
		dependabotConfigFile.addGitHubCodeQlActionGroupIfMissing();
		assertEquals("""
				updates:
				- package-ecosystem: "github-actions"
				  groups:
				    github-codeql-action:
				      patterns:
				      - "github/codeql-action*"
				- package-ecosystem: "maven"
				""", dependabotConfigFile.asString());
	}

	@Test
	void testAddGitHubCodeQlActionGroupKeepsExistingGroups() throws Exception {
		var dependabotConfigFile = new DependabotConfigFile("""
				updates:
				- package-ecosystem: "github-actions"
				  groups:
				    docker-actions:
				      patterns:
				      - "docker/*"
				""");
		dependabotConfigFile.addGitHubCodeQlActionGroupIfMissing();
		assertEquals("""
				updates:
				- package-ecosystem: "github-actions"
				  groups:
				    docker-actions:
				      patterns:
				      - "docker/*"
				    github-codeql-action:
				      patterns:
				      - "github/codeql-action*"
				""", dependabotConfigFile.asString());
	}

	@Test
	void testAddGitHubCodeQlActionGroupIsIdempotent() throws Exception {
		var content = """
				updates:
				- package-ecosystem: "github-actions"
				  groups:
				    github-codeql-action:
				      patterns:
				      - "github/codeql-action-custom*"
				""";
		var dependabotConfigFile = new DependabotConfigFile(content);
		dependabotConfigFile.addGitHubCodeQlActionGroupIfMissing();
		assertEquals(content, dependabotConfigFile.asString());
	}

	@Test
	void testAddOpenPullRequestsLimitIfMissing() throws Exception {
		var dependabotConfigFile = new DependabotConfigFile("""
				updates:
				- package-ecosystem: "github-actions"
				""");
		dependabotConfigFile.addOpenPullRequestsLimitIfMissing();
		assertEquals("""
				updates:
				- package-ecosystem: "github-actions"
				  open-pull-requests-limit: 10
				""", dependabotConfigFile.asString());
	}

	@Test
	void testAddOpenPullRequestsLimitIfTooLow() throws Exception {
		var dependabotConfigFile = new DependabotConfigFile("""
				updates:
				- package-ecosystem: "github-actions"
				  open-pull-requests-limit: 1
				""");
		dependabotConfigFile.addOpenPullRequestsLimitIfMissing();
		assertEquals("""
				updates:
				- package-ecosystem: "github-actions"
				  open-pull-requests-limit: 10
				""", dependabotConfigFile.asString());
	}

	@Test
	void testAddOpenPullRequestsLimitKeepsHigherLimit() throws Exception {
		var content = """
				updates:
				- package-ecosystem: "github-actions"
				  open-pull-requests-limit: 15
				""";
		var dependabotConfigFile = new DependabotConfigFile(content);
		dependabotConfigFile.addOpenPullRequestsLimitIfMissing();
		assertEquals(content, dependabotConfigFile.asString());
	}

	@Test
	void testAddCooldownIfTooLow() throws Exception {
		var dependabotConfigFile = new DependabotConfigFile("""
				updates:
				- package-ecosystem: "github-actions"
				  cooldown:
				    default-days: 2
				""");
		dependabotConfigFile.addCooldownIfMissing();
		assertEquals("""
				updates:
				- package-ecosystem: "github-actions"
				  cooldown:
				    default-days: 7
				""", dependabotConfigFile.asString());
	}

}
