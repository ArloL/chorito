package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

public final class JavaDirectoryStream {

	private JavaDirectoryStream() {
	}

	public static Stream<Path> javaDirectories(ChoreContext context) {
		return Stream.of(mavenPoms(context), javaBuildGradles(context))
				.flatMap(Function.identity())
				.map(MyPaths::getParent)
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

	public static Stream<Path> javaBuildGradles(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(
						file -> file.endsWith("build.gradle")
								&& FilesSilent.readString(file).contains("java")
				);
	}

}
