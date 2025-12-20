package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ExecutableFlagger;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.MyPaths;

public class MavenWrapperChore implements Chore {

	private static Logger LOG = LoggerFactory
			.getLogger(MavenWrapperChore.class);

	private static String DEFAULT_PROPERTIES = """
			wrapperVersion=3.3.4
			distributionType=bin
			distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.12/apache-maven-3.9.12-bin.zip
			wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
		LOG.info("Running MavenWrapperChore");
		DirectoryStreams.rootMavenPoms(context)
				.map(MyPaths::getParent)
				.forEach(pomDir -> {
					Path wrapper = pomDir.resolve("mvnw");
					Path wrapperJar = pomDir
							.resolve(".mvn/wrapper/maven-wrapper.jar");
					Path wrapperProperties = pomDir
							.resolve(".mvn/wrapper/maven-wrapper.properties");
					if (!FilesSilent.exists(wrapper)
							|| !FilesSilent.exists(wrapperJar)) {
						LOG.info("Running mvn wrapper:3.3.4:wrapper");
						context.newProcessBuilder(
								"mvn",
								"-N",
								"wrapper:3.3.4:wrapper",
								"-Dmaven=3.9.12",
								"-Dtype=bin"
						)
								.inheritIO()
								.directory(pomDir)
								.start()
								.waitFor(5, TimeUnit.MINUTES);
						context.setDirty();
					}
					if (!FilesSilent.exists(wrapperJar)) {
						throw new IllegalStateException("No maven-wrapper.jar");
					}
					ExecutableFlagger.makeExecutableIfPossible(wrapper);
					if (FilesSilent.exists(wrapperProperties)) {
						String content = FilesSilent
								.readString(wrapperProperties);
						if (!DEFAULT_PROPERTIES.equals(content)) {
							LOG.info("Running ./mvnw wrapper::wrapper");
							context.newProcessBuilder(
									"./mvnw",
									"-N",
									"wrapper:3.3.4:wrapper",
									"-Dmaven=3.9.12",
									"-Dtype=bin"
							)
									.inheritIO()
									.directory(pomDir)
									.start()
									.waitFor(5, TimeUnit.MINUTES);
							context.setDirty();
						}
					}
				});
		return context;
	}

}
