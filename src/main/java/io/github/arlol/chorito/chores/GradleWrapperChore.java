package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ExecutableFlagger;
import io.github.arlol.chorito.tools.FilesSilent;

public class GradleWrapperChore implements Chore {

	private static Logger LOG = LoggerFactory
			.getLogger(GradleWrapperChore.class);

	private static String DEFAULT_PROPERTIES = """
			distributionBase=GRADLE_USER_HOME
			distributionPath=wrapper/dists
			distributionUrl=https\\://services.gradle.org/distributions/gradle-8.0.2-all.zip
			networkTimeout=10000
			zipStoreBase=GRADLE_USER_HOME
			zipStorePath=wrapper/dists
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
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
						"8.0.2",
						"--distribution-type",
						"all",
						"--no-daemon"
				).inheritIO().start().waitFor(5, TimeUnit.MINUTES);
				context.newProcessBuilder(
						"./gradlew",
						"wrapper",
						"--gradle-version",
						"8.0.2",
						"--distribution-type",
						"all",
						"--no-daemon"
				).inheritIO().start().waitFor(5, TimeUnit.MINUTES);
			}
		}
		return context;
	}

}
