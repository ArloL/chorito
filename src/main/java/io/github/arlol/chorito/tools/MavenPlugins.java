package io.github.arlol.chorito.tools;

/**
 * The Maven plugins chorito recognises in a pom, named once so the chores that
 * find, anchor against and insert them all mean the same thing by "the
 * formatter plugin".
 */
public abstract class MavenPlugins {

	/**
	 * What identifies a plugin inside a pom. {@link MavenPomFile} turns this
	 * into the selector that locates it, so no chore has to spell one out.
	 */
	public record Id(
			String groupId,
			String artifactId
	) {

		@Override
		public String toString() {
			return groupId + ":" + artifactId;
		}

	}

	public static final Id FORMATTER = new Id(
			"net.revelc.code.formatter",
			"formatter-maven-plugin"
	);
	public static final Id FLATTEN = new Id(
			"org.codehaus.mojo",
			"flatten-maven-plugin"
	);
	public static final Id SPOTBUGS = new Id(
			"com.github.spotbugs",
			"spotbugs-maven-plugin"
	);
	public static final Id MODERNIZER = new Id(
			"org.gaul",
			"modernizer-maven-plugin"
	);
	public static final Id MAVEN_SOURCE = new Id(
			"org.apache.maven.plugins",
			"maven-source-plugin"
	);
	public static final Id LIFECYCLE_MAPPING = new Id(
			"org.eclipse.m2e",
			"lifecycle-mapping"
	);

	private MavenPlugins() {
	}

}
