package io.github.arlol.chorito.chores;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.concurrent.TimeUnit;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class MavenWrapperChore {

	private static String DEFAULT_PROPERTIES = """
			distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.4/apache-maven-3.8.4-bin.zip
			wrapperUrl=https://repo.maven.apache.org/maven2/io/takari/maven-wrapper/0.5.6/maven-wrapper-0.5.6.jar
			""";

	private final ChoreContext context;

	public MavenWrapperChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path wrapper = context.resolve("mvnw");
		if (FilesSilent.exists(wrapper)) {
			var permissions = FilesSilent.getPosixFilePermissions(wrapper);
			permissions.add(PosixFilePermission.OWNER_EXECUTE);
			permissions.add(PosixFilePermission.GROUP_EXECUTE);
			permissions.add(PosixFilePermission.OTHERS_EXECUTE);
			FilesSilent.setPosixFilePermissions(wrapper, permissions);
		}
		Path path = context.resolve(".mvn/wrapper/maven-wrapper.properties");
		if (FilesSilent.exists(path)) {
			String content = FilesSilent.readString(path);
			if (!DEFAULT_PROPERTIES.equals(content)) {
				try {
					new ProcessBuilder(
							"./mvnw",
							"-N",
							"io.takari:maven:wrapper",
							"-Dmaven=3.8.4"
					).start().waitFor(2, TimeUnit.MINUTES);
				} catch (InterruptedException | IOException e) {
					throw new IllegalStateException(e);
				}
			}
		}
	}

}
