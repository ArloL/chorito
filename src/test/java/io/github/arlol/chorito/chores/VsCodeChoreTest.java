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
		FilesSilent.touch(extension.root().resolve(".vscode/settings.json"));

		// when
		doit();

		// then
		Path extensions = extension.root().resolve(".vscode/extensions.json");
		assertThat(FilesSilent.exists(extensions)).isTrue();
		assertThat(extensions).content().isEqualTo("""
				{
				    "recommendations": [
				        "editorconfig.editorconfig"
				    ]
				}
				""");
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
				    ]
				}
				""");
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
				    ]
				}
				""");
	}

}
