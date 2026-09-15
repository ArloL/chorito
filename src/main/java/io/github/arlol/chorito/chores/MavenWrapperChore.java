package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ExecutableFlagger;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.MavenVersions;
import io.github.arlol.chorito.tools.MyPaths;

public class MavenWrapperChore implements Chore {

	private static Logger LOG = LoggerFactory
			.getLogger(MavenWrapperChore.class);

	/**
	 * The distribution every managed wrapper points at, named by the same
	 * constant {@link EnforcerPluginChore} writes into the pom's
	 * {@code requireMavenVersion}. A wrapper that installs an older Maven than
	 * the pom demands fails every build in the repository, so the two are not
	 * free to move apart: bumping {@link MavenVersions#MAVEN} moves both, and
	 * the next chorito run brings each managed wrapper along.
	 * <p>
	 * Substituted rather than formatted because the properties file the wrapper
	 * writes ends its lines with {@code \n} on every platform, which is what a
	 * format string may not say.
	 */
	private static String DEFAULT_PROPERTIES = """
			wrapperVersion=3.3.4
			distributionType=bin
			distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${maven}/apache-maven-${maven}-bin.zip
			wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar
			"""
			.replace("${maven}", MavenVersions.MAVEN);

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
								"-Dmaven=" + MavenVersions.MAVEN,
								"-Dtype=bin"
						)
								.inheritIO()
								.directory(pomDir)
								.start()
								.waitFor(5, TimeUnit.MINUTES);
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
									"-Dmaven=" + MavenVersions.MAVEN,
									"-Dtype=bin"
							)
									.inheritIO()
									.directory(pomDir)
									.start()
									.waitFor(5, TimeUnit.MINUTES);
						}
					}
				});
		return context;
	}

}
