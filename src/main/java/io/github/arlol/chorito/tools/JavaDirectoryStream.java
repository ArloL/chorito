package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

public final class JavaDirectoryStream {

	private JavaDirectoryStream() {
	}

	public static Stream<Path> javaDirectories(ChoreContext context) {
		return Stream.of(mavenDirectories(context), gradleDirectories(context))
				.flatMap(Function.identity())
				.distinct();
	}

	public static Stream<Path> mavenDirectories(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(file -> file.endsWith("pom.xml"))
				.map(MyPaths::getParent);
	}

	public static Stream<Path> gradleDirectories(ChoreContext context) {
		return context.textFiles()
				.stream()
				.filter(
						file -> file.endsWith("build.gradle")
								&& FilesSilent.readString(file).contains("java")
				)
				.map(MyPaths::getParent);
	}

}
