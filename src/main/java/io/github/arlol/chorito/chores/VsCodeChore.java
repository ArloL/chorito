package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsonBuilder;
import io.github.arlol.chorito.tools.Jsons;
import io.github.arlol.chorito.tools.MyPaths;

public class VsCodeChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Stream.of(
				DirectoryStreams.javaDirs(context),
				context.textFiles()
						.stream()
						.map(MyPaths::getParent)
						.filter(file -> file.endsWith(".vscode"))
						.map(MyPaths::getParent)
		).flatMap(Function.identity()).distinct().forEach(dir -> {
			Path settings = dir.resolve(".vscode/settings.json");
			Path extensions = dir.resolve(".vscode/extensions.json");

			if (FilesSilent.anyNotExists(settings, extensions)) {
				context.setDirty();
			}

			if (FilesSilent.anyChildExists(
					dir,
					"mvnw",
					"pom.xml",
					"gradlew",
					"build.gradle"
			)) {
				String newSettingsJson = newSettingsJson(settings);
				FilesSilent.writeString(settings, newSettingsJson);
			}

			var extensionsContent = FilesSilent.exists(extensions)
					? FilesSilent.readString(extensions)
					: "";

			var builder = extensionsContent == "" ? JsonBuilder.object()
					: JsonBuilder.wrap("");

			if (FilesSilent.anyChildExists(dir, "mvnw", "pom.xml")) {
				builder.arrayAdd("recommendations", "vscjava.vscode-java-pack");
			}

			if (FilesSilent.anyChildExists(dir, "gradlew", "build.gradle")) {
				builder.arrayAdd(
						"recommendations",
						"vscjava.vscode-gradle",
						"vscjava.vscode-java-pack"
				);
			}

			var newExtensionsContent = builder
					.arrayAdd("recommendations", "editorconfig.editorconfig")
					.arrayDistinctSort("recommendations")
					.asString();

			if (!extensionsContent.equals(newExtensionsContent)) {
				FilesSilent.writeString(extensions, newExtensionsContent);
			}

		});
		return context;
	}

	private String newSettingsJson(Path settings) {
		ObjectNode template = (ObjectNode) Jsons
				.parse(
						ClassPathFiles
								.readString("vscode-settings/settings.json")
				)
				.orElseThrow(IllegalStateException::new);
		if (!FilesSilent.exists(settings)) {
			return JsonBuilder.wrap(template).asString();
		}
		Jsons.parse(settings).ifPresent(node -> {
			if (node instanceof ObjectNode objectNode) {
				if (objectNode.has(
						"java.compile.nullAnalysis.nullable"
				) && objectNode.get("java.compile.nullAnalysis.nullable").toString().equalsIgnoreCase("[\"jakarta.annotation.Nullable\",\"edu.umd.cs.findbugs.annotations.Nullable\",\"javax.annotation.Nullable\",\"javax.annotation.CheckForNull\",\"org.jspecify.annotations.Nullable\",\"org.checkerframework.checker.nullness.qual.Nullable\",\"org.checkerframework.checker.nullness.compatqual.NullableDecl\",\"org.checkerframework.checker.nullness.compatqual.NullableType\",\"org.springframework.lang.Nullable\",\"android.support.annotation.Nullable\",\"androidx.annotation.Nullable\",\"androidx.annotation.RecentlyNullable\",\"com.android.annotations.Nullable\",\"org.eclipse.jdt.annotation.Nullable\",\"org.jetbrains.annotations.Nullable\"]")) {
					objectNode.remove("java.compile.nullAnalysis.nullable");
				}
				if (objectNode.has(
						"java.compile.nullAnalysis.nonnull"
				) && objectNode.get("java.compile.nullAnalysis.nonnull").toString().equalsIgnoreCase("[\"jakarta.annotation.Nonnull\",\"edu.umd.cs.findbugs.annotations.NonNull\",\"javax.annotation.Nonnull\",\"lombok.NonNull\",\"org.jspecify.annotations.NonNull\",\"org.checkerframework.checker.nullness.qual.NonNull\",\"org.checkerframework.checker.nullness.compatqual.NonNullDecl\",\"org.checkerframework.checker.nullness.compatqual.NonNullType\",\"org.springframework.lang.NonNull\",\"android.support.annotation.NonNull\",\"androidx.annotation.NonNull\",\"androidx.annotation.RecentlyNonNull\",\"com.android.annotations.NonNull\",\"org.eclipse.jdt.annotation.NonNull\",\"org.jetbrains.annotations.NotNull\"]")) {
					objectNode.remove("java.compile.nullAnalysis.nonnull");
				}
			}
			Jsons.merge(template, node);
		});
		return JsonBuilder.wrap(template).asString();
	}

}
