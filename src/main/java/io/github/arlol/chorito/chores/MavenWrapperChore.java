package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
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
	 * the next chorito run brings each managed wrapper along.
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

	private static final String DISTRIBUTION_ARTIFACT = "org.apache.maven:apache-maven:"
			+ MavenVersions.MAVEN + ":zip:bin";

	private static final String DISTRIBUTION_FILE = "apache-maven-"
			+ MavenVersions.MAVEN + "-bin.zip";

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
					if (FilesSilent.exists(wrapperProperties) && !isCurrent(
							FilesSilent.readString(wrapperProperties)
					)) {
						generateWrapper(context, pomDir, "./mvnw");
					}
				});
		return context;
	}

	/**
	 * Whether the wrapper is on the declared Maven and pins its checksum.
	 * <p>
	 * The checksum is taken as it stands rather than checked: checking it would
	 * download the distribution on every run, and a wrong one fails the
	 * wrapper's first download anyway. Renovate recomputes it when it bumps the
	 * version, but only in a file that already has one -- which is why this
	 * adds it.
	 */
	private static boolean isCurrent(String properties) {
		return DISTRIBUTION_SHA256_SUM.matcher(properties).find()
				&& DISTRIBUTION_SHA256_SUM.matcher(properties)
						.replaceAll("")
						.equals(DEFAULT_PROPERTIES);
	}

	private static void generateWrapper(
			ChoreContext context,
			Path pomDir,
			String maven
	) {
		String sha256 = distributionSha256(context, pomDir, maven);
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

	/**
	 * Has Maven fetch the distribution and hashes it.
	 * <p>
	 * Central publishes SHA-1 and SHA-512 for it but no SHA-256, the one digest
	 * the wrapper checks, so it has to be computed. Maven rather than a plain
	 * download: its resolver retries when Central rate limits, and verifies
	 * what it fetched against the published SHA-1, so the SHA-256 is of the
	 * distribution Central actually serves.
	 * <p>
	 * The copy lands in a directory of its own inside the project, where the
	 * process and this chore see the same path, and is deleted again so the
	 * next commit does not pick it up.
	 */
	private static String distributionSha256(
			ChoreContext context,
			Path pomDir,
			String maven
	) {
		Path directory = FilesSilent
				.createTempDirectory(pomDir, ".chorito-maven-");
		Path distribution = directory.resolve(DISTRIBUTION_FILE);
		try {
			LOG.info("Downloading {} to hash it", DISTRIBUTION_ARTIFACT);
			context.newProcessBuilder(
					maven,
					"-N",
					"dependency:3.11.0:copy",
					"-Dartifact=" + DISTRIBUTION_ARTIFACT,
					"-DoutputDirectory=" + directory.toAbsolutePath()
			)
					.inheritIO()
					.directory(pomDir)
					.start()
					.waitFor(5, TimeUnit.MINUTES);
			if (!FilesSilent.exists(distribution)) {
				throw new IllegalStateException(
						"Could not download " + DISTRIBUTION_ARTIFACT
				);
			}
			return HexFormat.of()
					.formatHex(
							MessageDigest.getInstance("SHA-256")
									.digest(
											FilesSilent
													.readAllBytes(distribution)
									)
					);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		} finally {
			FilesSilent.deleteIfExists(distribution);
			FilesSilent.deleteIfExists(directory);
		}
	}

}
