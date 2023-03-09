package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.ExecutableFlagger;

public class GradleWrapperChore {

	private static Logger LOG = LoggerFactory
			.getLogger(GradleWrapperChore.class);

	private static String DEFAULT_PROPERTIES = """
			distributionBase=GRADLE_USER_HOME
			distributionPath=wrapper/dists
			distributionUrl=https\\://services.gradle.org/distributions/gradle-7.5.1-all.zip
			zipStoreBase=GRADLE_USER_HOME
			zipStorePath=wrapper/dists
			""";

	private final ChoreContext context;

	public GradleWrapperChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		LOG.info("Running GradleWrapperChore");
		Path wrapper = context.resolve("gradlew");
		if (FilesSilent.exists(wrapper)) {
			ExecutableFlagger.makeExecutableIfPossible(wrapper);
		}
		Path path = context.resolve("gradle/wrapper/gradle-wrapper.properties");
		if (FilesSilent.exists(path)) {
			String content = FilesSilent.readString(path);
			if (!DEFAULT_PROPERTIES.equals(content)) {
				LOG.info("Running ./gradlew wrapper");
				context.newProcessBuilder(
						"./gradlew",
						"wrapper",
						"--gradle-version",
						"7.5.1",
						"--distribution-type",
						"all",
						"--no-daemon"
				).inheritIO().start().waitFor(5, TimeUnit.MINUTES);
				context.newProcessBuilder(
						"./gradlew",
						"wrapper",
						"--gradle-version",
						"7.5.1",
						"--distribution-type",
						"all",
						"--no-daemon"
				).inheritIO().start().waitFor(5, TimeUnit.MINUTES);
			}
		}
	}

}
