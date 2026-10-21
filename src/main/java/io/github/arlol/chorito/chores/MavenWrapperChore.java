package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	 * the next chorito run brings each older managed wrapper along. A newer one
	 * is Renovate's doing and stays.
	 * <p>
	 * Substituted rather than formatted because the properties file the wrapper
	 * writes ends its lines with {@code \n} on every platform, which is what a
	 * format string may not say.
	 * <p>
	 * The wrapper also writes a {@code distributionSha256Sum} line after the
	 * {@code distributionUrl}. It is left out here because only a download can
	 * say what it is; see {@link #isCurrent(String)}.
	 */
	private static String DEFAULT_PROPERTIES = """
			wrapperVersion=3.3.4
			distributionType=bin
			distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${maven}/apache-maven-${maven}-bin.zip
			wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar
			"""
			.replace("${maven}", MavenVersions.MAVEN);

	private static final Pattern DISTRIBUTION_SHA256_SUM = Pattern
			.compile("(?m)^distributionSha256Sum=[^\n]*\n");

	private static final Pattern DISTRIBUTION_VERSION = Pattern
			.compile("/apache-maven/([^/\n]+)/apache-maven-\\1-bin\\.zip");

	private static final Pattern WRAPPER_VERSION = Pattern
			.compile("(?m)^wrapperVersion=3\\.3\\.4$");

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
						generateWrapper(context, pomDir, "mvn");
					}
					if (!FilesSilent.exists(wrapperJar)) {
						throw new IllegalStateException("No maven-wrapper.jar");
					}
					ExecutableFlagger.makeExecutableIfPossible(wrapper);
					if (!FilesSilent.exists(wrapperProperties)) {
						return;
					}
					String properties = FilesSilent
							.readString(wrapperProperties);
					if (isCurrent(properties)) {
						return;
					}
					if (WRAPPER_VERSION.matcher(properties).find()) {
						FilesSilent.writeString(
								wrapperProperties,
								DEFAULT_PROPERTIES.replace(
										"-bin.zip\n",
										"-bin.zip\ndistributionSha256Sum="
												+ distributionSha256(context)
												+ "\n"
								)
						);
					} else {
						generateWrapper(context, pomDir, "./mvnw");
					}
				});
		return context;
	}

	/**
	 * Whether the wrapper is on at least the declared Maven and pins its
	 * checksum.
	 * <p>
	 * The checksum is taken as it stands rather than checked: checking it would
	 * download the distribution on every run, and a wrong one fails the
	 * wrapper's first download anyway. Renovate recomputes it when it bumps the
	 * version, but only in a file that already has one -- which is why this
	 * adds it.
	 */
	private static boolean isCurrent(String properties) {
		Matcher version = DISTRIBUTION_VERSION.matcher(properties);
		if (!DISTRIBUTION_SHA256_SUM.matcher(properties).find()
				|| !version.find() || !isAtLeastDeclared(version.group(1))) {
			return false;
		}
		String declared = properties.replace(
				version.group(),
				"/apache-maven/" + MavenVersions.MAVEN + "/apache-maven-"
						+ MavenVersions.MAVEN + "-bin.zip"
		);
		return DISTRIBUTION_SHA256_SUM.matcher(declared)
				.replaceAll("")
				.equals(DEFAULT_PROPERTIES);
	}

	/**
	 * Numeric parts only: a pre-release such as {@code 4.0.0-rc-4} does not
	 * parse and is replaced with the declared release.
	 */
	private static boolean isAtLeastDeclared(String version) {
		String[] actual = version.split("\\.");
		String[] declared = MavenVersions.MAVEN.split("\\.");
		try {
			for (int i = 0; i < Math.max(actual.length, declared.length); i++) {
				int compared = Integer.compare(
						i < actual.length ? Integer.parseInt(actual[i]) : 0,
						i < declared.length ? Integer.parseInt(declared[i]) : 0
				);
				if (compared != 0) {
					return compared > 0;
				}
			}
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Runs Maven, which needs a JDK the chores runner lacks. Only for a wrapper
	 * that is missing or on another wrapper version, since its scripts and jar
	 * need replacing too; a wrapper already on 3.3.4 only has its properties
	 * rewritten.
	 */
	private static void generateWrapper(
			ChoreContext context,
			Path pomDir,
			String maven
	) {
		String sha256 = distributionSha256(context);
		LOG.info("Running {} wrapper:3.3.4:wrapper", maven);
		context.newProcessBuilder(
				maven,
				"-N",
				"wrapper:3.3.4:wrapper",
				"-Dmaven=" + MavenVersions.MAVEN,
				"-Dtype=bin",
				"-DdistributionSha256Sum=" + sha256
		).inheritIO().directory(pomDir).start().waitFor(5, TimeUnit.MINUTES);
	}

	private static String distributionSha256(ChoreContext context) {
		LOG.info("Downloading Maven {} to hash it", MavenVersions.MAVEN);
		return context.mavenDistributionSha256(MavenVersions.MAVEN)
				.orElseThrow(
						() -> new IllegalStateException(
								"Could not download Maven "
										+ MavenVersions.MAVEN
						)
				);
	}

}
