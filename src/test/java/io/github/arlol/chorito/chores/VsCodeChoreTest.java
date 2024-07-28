package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class VsCodeChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new VsCodeChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		// given
		FilesSilent.touch(extension.root().resolve(".vscode/lol.txt"));

		// when
		doit();

		// then
		Path extensions = extension.root().resolve(".vscode/extensions.json");
		assertThat(FilesSilent.exists(extensions)).isTrue();
		assertThat(extensions).content().isEqualTo("""
				{
				    "recommendations": [
				        "editorconfig.editorconfig"
				    ],
				}
				""");
		Path settings = extension.root().resolve(".vscode/settings.json");
		assertThat(FilesSilent.exists(settings)).isFalse();
	}

	@Test
	public void testWithPom() {
		// given
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		// when
		doit();

		// then
		Path extensions = extension.root().resolve(".vscode/extensions.json");
		assertThat(FilesSilent.exists(extensions)).isTrue();
		assertThat(extensions).content().isEqualTo("""
				{
				    "recommendations": [
				        "editorconfig.editorconfig",
				        "vscjava.vscode-java-pack"
				    ],
				}
				""");
		Path settings = extension.root().resolve(".vscode/settings.json");
		assertThat(FilesSilent.exists(settings)).isTrue();
		assertThat(settings).content()
				.isEqualTo(
						"""
								{
								    "editor.foldingImportsByDefault": true,
								    "java.compile.nullAnalysis.mode": "automatic",
								    "java.configuration.updateBuildConfiguration": "interactive",
								    "java.sources.organizeImports.starThreshold": 30,
								    "java.sources.organizeImports.staticStarThreshold": 30,
								    "[java]": {
								        "editor.formatOnSave": true,
								        "editor.codeActionsOnSave": {
								            "source.organizeImports": "always",
								        },
								    },
								}
								"""
				);
	}

	@Test
	public void testWithExistingExtension() {
		// given
		Path extensions = extension.root().resolve(".vscode/extensions.json");
		FilesSilent.writeString(extensions, """
				{"recommendations": ["kisstkondoros.vscode-codemetrics"]}
				""");

		// when
		doit();

		// then
		assertThat(FilesSilent.exists(extensions)).isTrue();
		assertThat(extensions).content().isEqualTo("""
				{
				    "recommendations": [
				        "editorconfig.editorconfig",
				        "kisstkondoros.vscode-codemetrics"
				    ],
				}
				""");
		Path settings = extension.root().resolve(".vscode/settings.json");
		assertThat(FilesSilent.exists(settings)).isFalse();
	}

	@Test
	public void testWithExistingSettings() {
		// given
		FilesSilent.touch(extension.root().resolve("pom.xml"));
		Path settings = extension.root().resolve(".vscode/settings.json");
		FilesSilent.writeString(settings, """
				{
					"java.format.settings.url": ".vscode/java-formatter.xml",
				}
				""");

		// when
		doit();

		// then
		assertThat(settings).content()
				.isEqualTo(
						"""
								{
								    "[java]": {
								        "editor.formatOnSave": true,
								        "editor.codeActionsOnSave": {
								            "source.organizeImports": "always",
								        },
								    },
								    "editor.foldingImportsByDefault": true,
								    "java.compile.nullAnalysis.mode": "automatic",
								    "java.configuration.updateBuildConfiguration": "interactive",
								    "java.format.settings.url": ".vscode/java-formatter.xml",
								    "java.sources.organizeImports.starThreshold": 30,
								    "java.sources.organizeImports.staticStarThreshold": 30,
								}
								"""
				);
	}

}
