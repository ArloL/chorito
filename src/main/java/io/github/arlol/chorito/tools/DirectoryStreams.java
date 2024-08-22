package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

public final class DirectoryStreams {

	private DirectoryStreams() {
	}

	public static Stream<Path> javaDirectories(ChoreContext context) {
		return Stream
				.of(
						mavenPoms(context).map(MyPaths::getParent),
						javaGradleDir(context)
				)
				.flatMap(Function.identity())
				.distinct();
	}

	public static Stream<Path> mavenPoms(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(file -> file.endsWith("pom.xml"));
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

	public static Stream<Path> mavenPomsWithSourceCode(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(file -> file.endsWith("pom.xml"))
				.filter(pom -> {
					Document doc = JsoupSilent
							.parse(pom, "UTF-8", "", Parser.xmlParser());
					Element relativePath = doc
							.selectFirst("project > parent > relativePath");
					return relativePath == null || !relativePath.hasText();
				})
				.filter(pom -> {
					var main = pom.resolveSibling("src/main/java");
					var test = pom.resolveSibling("src/test/java");
					return context.textFiles()
							.stream()
							.anyMatch(
									file -> file.startsWith(main)
											|| file.startsWith(test)
							);
				});
	}

	public static Stream<Path> rootGradleDir(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(file -> file.endsWith("settings.gradle"))
				.map(MyPaths::getParent);
	}

	public static Stream<Path> rootJavaGradleDir(ChoreContext context) {
		return rootGradleDir(context).filter(dir -> {
			Path buildGradle = dir.resolve("build.gradle");
			return FilesSilent.exists(buildGradle)
					&& FilesSilent.readString(buildGradle).contains("java");
		});
	}

	public static Stream<Path> gradleDir(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(
						file -> file.endsWith("build.gradle")
								|| file.endsWith("settings.gradle")
				)
				.map(MyPaths::getParent)
				.distinct();
	}

	public static Stream<Path> javaGradleDir(ChoreContext context) {
		return gradleDir(context).filter(dir -> {
			Path buildGradle = dir.resolve("build.gradle");
			return FilesSilent.exists(buildGradle)
					&& FilesSilent.readString(buildGradle).contains("java");
		});
	}

	public static Stream<Path> nodeDir(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(file -> file.endsWith("package.json"))
				.map(MyPaths::getParent);
	}

}
