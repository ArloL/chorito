package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.ExecutableFlagger;
import io.github.arlol.chorito.tools.FilesSilent;

public class GradleWrapperChore implements Chore {

	private static Logger LOG = LoggerFactory
			.getLogger(GradleWrapperChore.class);

	private static String DEFAULT_PROPERTIES = """
			distributionBase=GRADLE_USER_HOME
			distributionPath=wrapper/dists
			distributionUrl=https\\://services.gradle.org/distributions/gradle-8.10-all.zip
			networkTimeout=10000
			validateDistributionUrl=true
			zipStoreBase=GRADLE_USER_HOME
			zipStorePath=wrapper/dists
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
		LOG.info("Running GradleWrapperChore");
		DirectoryStreams.rootGradleDir(context).forEach(gradleDir -> {
			Path wrapper = gradleDir.resolve("gradlew");
			Path wrapperJar = gradleDir
					.resolve("gradle/wrapper/gradle-wrapper.jar");
			Path wrapperProperties = gradleDir
					.resolve("gradle/wrapper/gradle-wrapper.properties");

			if (!FilesSilent.exists(wrapper)
					|| !FilesSilent.exists(wrapperJar)) {
				LOG.info("Running ./gradlew wrapper");
				context.newProcessBuilder(
						"./gradlew",
						"wrapper",
						"--gradle-version",
						"8.10",
						"--distribution-type",
						"all",
						"--no-daemon"
				)
						.directory(gradleDir)
						.inheritIO()
						.start()
						.waitFor(5, TimeUnit.MINUTES);
				context.setDirty();
			}
			ExecutableFlagger.makeExecutableIfPossible(wrapper);
			if (FilesSilent.exists(wrapperProperties)) {
				String content = FilesSilent.readString(wrapperProperties);
				if (!DEFAULT_PROPERTIES.equals(content)) {
					context.newProcessBuilder(
							"./gradlew",
							"wrapper",
							"--gradle-version",
							"8.10",
							"--distribution-type",
							"all",
							"--no-daemon"
					)
							.directory(gradleDir)
							.inheritIO()
							.start()
							.waitFor(5, TimeUnit.MINUTES);
					context.setDirty();
				}
			}
		});
		return context;
	}

}
