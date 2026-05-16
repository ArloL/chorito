package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

public final class DirectoryStreams {

	private DirectoryStreams() {
	}

	public static Stream<Path> javaDirs(ChoreContext context) {
		return Stream
				.of(
						mavenPomsWithCode(context).map(MyPaths::getParent),
						javaGradleDirsWithCode(context)
				)
				.flatMap(Function.identity())
				.distinct();
	}

	public static Stream<Path> rootJavaDirs(ChoreContext context) {
		return Stream
				.of(
						rootMavenPomsWithCode(context).map(MyPaths::getParent),
						rootJavaGradleDirs(context)
				)
				.flatMap(Function.identity())
				.distinct();
	}

	public static Stream<Path> mavenPoms(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(file -> file.endsWith("pom.xml"));
	}

	public static Stream<Path> mavenPomsWithCode(ChoreContext context) {
		return mavenPoms(context)
				.filter(pom -> withCode(context, MyPaths.getParent(pom)));
	}

	public static Stream<Path> rootMavenPoms(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(file -> file.endsWith("pom.xml"))
				.filter(pom -> {
					Document doc = JsoupSilent
							.parse(pom, "UTF-8", "", Parser.xmlParser());
					Element relativePath = doc
							.selectFirst("project > parent > relativePath");
					return relativePath == null || !relativePath.hasText();
				});
	}

	public static Stream<Path> rootMavenPomsWithCode(ChoreContext context) {
		return rootMavenPoms(context).filter(rootPom -> {
			Path rootDir = MyPaths.getParent(rootPom);
			return mavenPomsWithCode(context).anyMatch(
					pom -> MyPaths.getParent(pom).startsWith(rootDir)
			);
		});
	}

	public static Stream<Path> gradleWrapperDirs(ChoreContext context) {
		var gradleWrapperStream = context.textFiles()
				.stream()
				.filter(file -> file.endsWith("gradlew"))
				.map(MyPaths::getParent);
		return Stream.of(gradleWrapperStream, rootGradleDirs(context))
				.flatMap(Function.identity())
				.distinct();
	}

	public static Stream<Path> rootGradleDirs(ChoreContext context) {
		return dirsContainingFile(context, "settings.gradle");
	}

	public static Stream<Path> rootJavaGradleDirs(ChoreContext context) {
		return rootGradleDirs(context).filter(dir -> {
			Path buildGradle = dir.resolve("build.gradle");
			return FilesSilent.exists(buildGradle)
					&& FilesSilent.readString(buildGradle).contains("java");
		});
	}

	public static Stream<Path> gradleDirs(ChoreContext context) {
		return dirsContainingFile(context, "build.gradle", "settings.gradle")
				.distinct();
	}

	public static Stream<Path> javaGradleDirsWithCode(ChoreContext context) {
		return gradleDirs(context).filter(dir -> {
			Path buildGradle = dir.resolve("build.gradle");
			return FilesSilent.exists(buildGradle)
					&& FilesSilent.readString(buildGradle).contains("java");
		}).filter(dir -> withCode(context, dir));
	}

	public static Stream<Path> pyprojectTomlDirs(ChoreContext context) {
		return dirsContainingFile(context, "pyproject.toml");
	}

	public static Stream<Path> buildZigDirs(ChoreContext context) {
		return dirsContainingFile(context, "build.zig");
	}

	public static Stream<Path> packageJsonDirs(ChoreContext context) {
		return dirsContainingFile(context, "package.json");
	}

	public static Stream<Path> jekyllGemfileDirs(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(file -> file.endsWith("Gemfile"))
				.filter(
						gemfile -> FilesSilent.readString(gemfile)
								.contains("jekyll")
				)
				.map(MyPaths::getParent);
	}

	public static Stream<Path> dotYarnDirs(ChoreContext context) {
		return context.textFiles()
				.stream()
				.map(start -> MyPaths.getParentPathWithName(start, ".yarn"))
				.flatMap(Optional::stream)
				.filter(
						dir -> FilesSilent.anyChildExists(
								MyPaths.getParent(dir),
								"package.json"
						)
				)
				.distinct();
	}

	private static Stream<Path> dirsContainingFile(
			ChoreContext context,
			String... fileNames
	) {
		return context.textFiles()
				.stream()
				.filter(
						file -> Arrays.stream(fileNames)
								.anyMatch(file::endsWith)
				)
				.map(MyPaths::getParent);
	}

	private static boolean withCode(ChoreContext context, Path dir) {
		var main = dir.resolve("src/main/java");
		var test = dir.resolve("src/test/java");
		return context.textFiles()
				.stream()
				.anyMatch(
						file -> file.startsWith(main) || file.startsWith(test)
				);
	}

}
