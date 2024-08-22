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
				        "editorconfig.editorconfig",
				    ],
				}
				""");
		Path settings = extension.root().resolve(".vscode/settings.json");
		assertThat(FilesSilent.exists(settings)).isFalse();
	}

	@Test
	public void testWithPom() {
		// given
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
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
				        "vscjava.vscode-java-pack",
				    ],
				}
				""");
		Path settings = extension.root().resolve(".vscode/settings.json");
		assertThat(FilesSilent.exists(settings)).isTrue();
		assertThat(settings).content()
				.isEqualTo(
						"""
								{
								    "[java]": {
								        "editor.foldingImportsByDefault": true,
								        "editor.formatOnSave": true,
								        "editor.codeActionsOnSave": {
								            "source.organizeImports": "always",
								        },
								    },
								    "java.compile.nullAnalysis.mode": "automatic",
								    "java.compile.nullAnalysis.nonnull": [
								        "jakarta.annotation.Nonnull",
								        "edu.umd.cs.findbugs.annotations.NonNull",
								        "javax.annotation.Nonnull",
								        "lombok.NonNull",
								        "org.jspecify.annotations.NonNull",
								        "org.checkerframework.checker.nullness.qual.NonNull",
								        "org.checkerframework.checker.nullness.compatqual.NonNullDecl",
								        "org.checkerframework.checker.nullness.compatqual.NonNullType",
								        "org.springframework.lang.NonNull",
								        "android.support.annotation.NonNull",
								        "androidx.annotation.NonNull",
								        "androidx.annotation.RecentlyNonNull",
								        "com.android.annotations.NonNull",
								        "org.eclipse.jdt.annotation.NonNull",
								        "org.jetbrains.annotations.NotNull",
								    ],
								    "java.compile.nullAnalysis.nonnullbydefault": [
								        "javax.annotation.ParametersAreNonnullByDefault",
								        "org.springframework.lang.NonNullApi",
								        "org.eclipse.jdt.annotation.NonNullByDefault",
								    ],
								    "java.compile.nullAnalysis.nullable": [
								        "jakarta.annotation.Nullable",
								        "edu.umd.cs.findbugs.annotations.Nullable",
								        "javax.annotation.Nullable",
								        "javax.annotation.CheckForNull",
								        "org.jspecify.annotations.Nullable",
								        "org.checkerframework.checker.nullness.qual.Nullable",
								        "org.checkerframework.checker.nullness.compatqual.NullableDecl",
								        "org.checkerframework.checker.nullness.compatqual.NullableType",
								        "org.springframework.lang.Nullable",
								        "android.support.annotation.Nullable",
								        "androidx.annotation.Nullable",
								        "androidx.annotation.RecentlyNullable",
								        "com.android.annotations.Nullable",
								        "org.eclipse.jdt.annotation.Nullable",
								        "org.jetbrains.annotations.Nullable",
								    ],
								    "java.configuration.updateBuildConfiguration": "interactive",
								    "java.sources.organizeImports.starThreshold": 30,
								    "java.sources.organizeImports.staticStarThreshold": 30,
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
				        "kisstkondoros.vscode-codemetrics",
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
								        "editor.foldingImportsByDefault": true,
								        "editor.formatOnSave": true,
								        "editor.codeActionsOnSave": {
								            "source.organizeImports": "always",
								        },
								    },
								    "java.compile.nullAnalysis.mode": "automatic",
								    "java.compile.nullAnalysis.nonnull": [
								        "jakarta.annotation.Nonnull",
								        "edu.umd.cs.findbugs.annotations.NonNull",
								        "javax.annotation.Nonnull",
								        "lombok.NonNull",
								        "org.jspecify.annotations.NonNull",
								        "org.checkerframework.checker.nullness.qual.NonNull",
								        "org.checkerframework.checker.nullness.compatqual.NonNullDecl",
								        "org.checkerframework.checker.nullness.compatqual.NonNullType",
								        "org.springframework.lang.NonNull",
								        "android.support.annotation.NonNull",
								        "androidx.annotation.NonNull",
								        "androidx.annotation.RecentlyNonNull",
								        "com.android.annotations.NonNull",
								        "org.eclipse.jdt.annotation.NonNull",
								        "org.jetbrains.annotations.NotNull",
								    ],
								    "java.compile.nullAnalysis.nonnullbydefault": [
								        "javax.annotation.ParametersAreNonnullByDefault",
								        "org.springframework.lang.NonNullApi",
								        "org.eclipse.jdt.annotation.NonNullByDefault",
								    ],
								    "java.compile.nullAnalysis.nullable": [
								        "jakarta.annotation.Nullable",
								        "edu.umd.cs.findbugs.annotations.Nullable",
								        "javax.annotation.Nullable",
								        "javax.annotation.CheckForNull",
								        "org.jspecify.annotations.Nullable",
								        "org.checkerframework.checker.nullness.qual.Nullable",
								        "org.checkerframework.checker.nullness.compatqual.NullableDecl",
								        "org.checkerframework.checker.nullness.compatqual.NullableType",
								        "org.springframework.lang.Nullable",
								        "android.support.annotation.Nullable",
								        "androidx.annotation.Nullable",
								        "androidx.annotation.RecentlyNullable",
								        "com.android.annotations.Nullable",
								        "org.eclipse.jdt.annotation.Nullable",
								        "org.jetbrains.annotations.Nullable",
								    ],
								    "java.configuration.updateBuildConfiguration": "interactive",
								    "java.format.settings.url": ".vscode/java-formatter.xml",
								    "java.sources.organizeImports.starThreshold": 30,
								    "java.sources.organizeImports.staticStarThreshold": 30,
								}
								"""
				);
	}

}
