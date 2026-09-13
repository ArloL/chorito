package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * The templates chorito ships are symlinks to the workflows it runs on itself,
 * so every capability chorito adopts is one edit away from being handed to
 * every managed repository. That has happened twice -- release attestation and
 * a GraalVM toolchain pin -- and both times the correction was written after
 * the leak had shipped.
 * <p>
 * These assertions are the guard rail: whatever chorito grants itself next
 * fails here instead.
 */
public class TemplateTest {

	private static final List<Supplier<GitHubActionsWorkflowFile>> TEMPLATES = List
			.of(
					Template::mainWorkflow,
					Template::choresWorkflow,
					Template::checkActionsWorkflow,
					Template::codeQlAnalysisWorkflow
			);

	@Test
	public void noTemplateHandsOutChoritosOwnPermissions() {
		for (Supplier<GitHubActionsWorkflowFile> template : TEMPLATES) {
			assertThat(template.get().asString())
					.doesNotContain("attestations:", "id-token:");
		}
	}

	@Test
	public void noTemplateHandsOutChoritosOwnJavaVersion() {
		for (Supplier<GitHubActionsWorkflowFile> template : TEMPLATES) {
			assertThat(template.get().getPinnedJavaVersion()).isEmpty();
		}
	}

	@Test
	public void choritosOwnWorkflowStillCarriesWhatWasStripped() {
		// Without this the assertions above pass just as happily once chorito
		// stops attesting its releases, and the guard rail quietly stops
		// guarding anything.
		String own = ClassPathFiles
				.readString("github-settings/workflows/main.yaml");

		assertThat(own).contains("attestations: write", "id-token: write");
		assertThat(Template.attestReleaseAssetsStep()).isPresent();
	}

	@Test
	public void generalisingLeavesThePermissionsAJobEarnsOnItsOwn() {
		assertThat(Template.mainWorkflow().asString())
				.contains("contents: write");
	}

}
