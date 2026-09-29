package io.github.arlol.chorito.chores;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FakeProcessBuilderSilent;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MavenVersions;
import io.github.arlol.chorito.tools.ProcessBuilderSilent;

public class MavenWrapperChoreTest {

	/**
	 * What the fake Maven hands out as the distribution. Anything will do: the
	 * chore only hashes it.
	 */
	private static final byte[] DISTRIBUTION = "not really a zip"
			.getBytes(UTF_8);

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new MavenWrapperChore().doit(extension.choreContext());

		assertThat(extension.relativePaths()).isEmpty();
	}

	@Test
	void testNada() throws Exception {
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(FakeProcessBuilderSilent.factory())
				.build();
		new MavenWrapperChore().doit(context);

		assertThat(extension.relativePaths()).isEmpty();
	}

	@Test
	void testCreateMavenWrapper() throws Exception {
		// given
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(this::fakeMaven)
				.build();
		Path pom = context.resolve("pom.xml");
		FilesSilent.touch(pom);

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertTrue(FilesSilent.exists(context.resolve("mvnw")));
	}

	@Test
	void testNestedMavenProjects() throws Exception {
		// given
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(this::fakeMaven)
				.build();
		FilesSilent.touch(context.resolve("pom.xml"));
		FilesSilent.writeString(context.resolve("nested/pom.xml"), """
				<project>
				<parent>
				<relativePath>..</relativePath>
				</parent>
				</project>
				""");

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertTrue(FilesSilent.exists(context.resolve("mvnw")));
		assertFalse(FilesSilent.exists(context.resolve("nested/mvnw")));
	}

	/**
	 * Renovate bumps {@link MavenVersions#MAVEN} and nothing else checks that
	 * this chore still interpolates it. A wrapper left on the old distribution
	 * installs a Maven older than the floor {@link EnforcerPluginChore} writes
	 * into the pom, which fails every build in the repository rather than
	 * quietly doing nothing.
	 */
	@Test
	void runsTheWrapperForTheDeclaredMavenVersion() throws Exception {
		// given
		List<String> commands = new ArrayList<>();
		var context = recordingContext(commands);
		FilesSilent.touch(context.resolve("pom.xml"));

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertThat(wrapperCommands(commands)).isNotEmpty()
				.allSatisfy(
						command -> assertThat(command)
								.contains("-Dmaven=" + MavenVersions.MAVEN)
				);
	}

	/**
	 * The checksum is of the distribution the wrapper is pointed at, so that is
	 * the one downloaded to hash.
	 */
	@Test
	void downloadsTheDistributionOfTheDeclaredMavenVersion() throws Exception {
		// given
		List<String> commands = new ArrayList<>();
		var context = recordingContext(commands);
		FilesSilent.touch(context.resolve("pom.xml"));

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertThat(commands).anySatisfy(
				command -> assertThat(command).contains(
						"-Dartifact=org.apache.maven:apache-maven:"
								+ MavenVersions.MAVEN + ":zip:bin"
				)
		);
	}

	/**
	 * Without distributionSha256Sum the wrapper installs whatever the
	 * repository hands it. With it, a truncated or substituted download fails
	 * instead of becoming the Maven every build runs.
	 */
	@Test
	void pinsTheDistributionChecksum() throws Exception {
		// given
		List<String> commands = new ArrayList<>();
		var context = recordingContext(commands);
		FilesSilent.touch(context.resolve("pom.xml"));

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertThat(wrapperCommands(commands)).isNotEmpty()
				.allSatisfy(
						command -> assertThat(command).contains(
								"-DdistributionSha256Sum="
										+ sha256(DISTRIBUTION)
						)
				);
	}

	@Test
	void addsTheChecksumToAWrapperWithoutOne() throws Exception {
		// given
		List<String> commands = new ArrayList<>();
		var context = recordingContext(commands);
		FilesSilent.touch(context.resolve("pom.xml"));
		FilesSilent.touch(context.resolve("mvnw"));
		FilesSilent.touch(context.resolve(".mvn/wrapper/maven-wrapper.jar"));
		FilesSilent.writeString(
				context.resolve(".mvn/wrapper/maven-wrapper.properties"),
				"""
						wrapperVersion=3.3.4
						distributionType=bin
						distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${maven}/apache-maven-${maven}-bin.zip
						wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar
						"""
						.replace("${maven}", MavenVersions.MAVEN)
		);

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertThat(wrapperCommands(commands)).singleElement()
				.satisfies(
						command -> assertThat(command).startsWith("./mvnw ")
								.contains(
										"-DdistributionSha256Sum="
												+ sha256(DISTRIBUTION)
								)
				);
	}

	/**
	 * The distribution is downloaded into the repository to be hashed, and must
	 * not be left there for the next commit to pick up.
	 */
	@Test
	void leavesNoDownloadBehind() throws Exception {
		// given
		var context = recordingContext(new ArrayList<>());
		FilesSilent.touch(context.resolve("pom.xml"));

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertThat(extension.relativePaths()).containsExactlyInAnyOrder(
				"pom.xml",
				"mvnw",
				"mvnw.cmd",
				".mvn",
				".mvn/wrapper",
				".mvn/wrapper/maven-wrapper.jar",
				".mvn/wrapper/maven-wrapper.properties"
		);
	}

	/**
	 * And the properties the chore compares against name the same version, so a
	 * wrapper already on it is left as it is instead of being rewritten on
	 * every run. Its checksum is taken as it stands: checking it would mean
	 * downloading the distribution on every run, and a wrong one fails the
	 * wrapper's first download loudly anyway.
	 */
	@Test
	void leavesAWrapperOnTheDeclaredMavenVersionAlone() throws Exception {
		// given
		List<String> commands = new ArrayList<>();
		var context = recordingContext(commands);
		FilesSilent.touch(context.resolve("pom.xml"));
		FilesSilent.touch(context.resolve("mvnw"));
		FilesSilent.touch(context.resolve(".mvn/wrapper/maven-wrapper.jar"));
		FilesSilent.writeString(
				context.resolve(".mvn/wrapper/maven-wrapper.properties"),
				"""
						wrapperVersion=3.3.4
						distributionType=bin
						distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${maven}/apache-maven-${maven}-bin.zip
						distributionSha256Sum=5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce
						wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar
						"""
						.replace("${maven}", MavenVersions.MAVEN)
		);

		// when
		new MavenWrapperChore().doit(context.refresh());

		// then
		assertThat(commands).isEmpty();
	}

	/**
	 * A context whose fake wrapper records the command line it was asked to
	 * run, so a test can assert on the version the chore passes rather than
	 * only on the files it leaves behind.
	 */
	private ChoreContext recordingContext(List<String> commands) {
		return extension.choreContext()
				.toBuilder()
				.processBuilderFactory(command -> {
					commands.add(String.join(" ", command));
					return fakeMaven(command);
				})
				.build();
	}

	private static List<String> wrapperCommands(List<String> commands) {
		return commands.stream()
				.filter(command -> command.contains(":wrapper"))
				.toList();
	}

	/**
	 * Stands in for both things the chore runs Maven for: copying the
	 * distribution out of the repository, and generating the wrapper.
	 */
	private ProcessBuilderSilent fakeMaven(String[] command) {
		var outputDirectory = Arrays.stream(command)
				.filter(argument -> argument.startsWith("-DoutputDirectory="))
				.map(argument -> argument.substring(argument.indexOf('=') + 1))
				.findFirst();
		if (outputDirectory.isPresent()) {
			return new FakeProcessBuilderSilent(command, processBuilder -> {
				Path directory = processBuilder.directory()
						.getFileSystem()
						.getPath(outputDirectory.orElseThrow());
				FilesSilent.write(
						directory.resolve(
								"apache-maven-" + MavenVersions.MAVEN
										+ "-bin.zip"
						),
						DISTRIBUTION
				);
			});
		}
		return new FakeProcessBuilderSilent(command, this::fakeMavenWrapper);
	}

	private static String sha256(byte[] bytes) throws Exception {
		return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
	}

	private void fakeMavenWrapper(ProcessBuilderSilent processBuilderSilent) {
		Path directory = processBuilderSilent.directory();
		FilesSilent.touch(directory.resolve("mvnw"));
		FilesSilent.touch(directory.resolve("mvnw.cmd"));
		FilesSilent.touch(directory.resolve(".mvn/wrapper/maven-wrapper.jar"));
		FilesSilent.touch(
				directory.resolve(".mvn/wrapper/maven-wrapper.properties")
		);
	}

}
