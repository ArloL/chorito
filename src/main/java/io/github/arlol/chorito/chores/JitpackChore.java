package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class JitpackChore {

	private static final String JAVA_JITPACK = """
			jdk:
			- openjdk21
			install:
			- ./mvnw --batch-mode install -DskipTests -Dsha1="${GIT_COMMIT}" -Drevision="${VERSION}"
			""";

	private ChoreContext context;

	public JitpackChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path pom = context.resolve("pom.xml");
		Path jitpack = context.resolve("jitpack.yml");
		if (FilesSilent.exists(pom) && !FilesSilent.exists(jitpack)) {
			FilesSilent.writeString(jitpack, JAVA_JITPACK);
		}
	}

}
