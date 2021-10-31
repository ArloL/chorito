package io.github.arlol.chorito.chores;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import io.github.arlol.chorito.tools.ChoreContext;

public class MavenWrapperChore {

	private static String DEFAULT_PROPERTIES = """
			distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.3/apache-maven-3.8.3-bin.zip
			wrapperUrl=https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar
			""";

	private final ChoreContext context;

	public MavenWrapperChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() throws Exception {
		Path path = context.resolve(".mvn/wrapper/maven-wrapper.properties");
		if (Files.exists(path)) {
			String content = Files.readString(path);
			if (!DEFAULT_PROPERTIES.equals(content)) {
				new ProcessBuilder(
						"./mvnw",
						"-N",
						"io.takari:maven:wrapper",
						"-Dmaven=3.8.3"
				).start().waitFor(2, TimeUnit.MINUTES);
			}
		}
	}

}
