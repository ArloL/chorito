package io.github.arlol.chorito.tools;

public abstract class JavaVersions {

	/**
	 * The Temurin release the jobs that build no native image pin. Renovate
	 * keeps it current through the custom manager in chorito's own
	 * renovate.json5.
	 */
	public static final String TEMURIN = "25.0.4";

	private JavaVersions() {
	}

	/**
	 * setup-java takes the JDK distribution from {@code .tool-versions}' vendor
	 * prefix and ignores its own {@code distribution} input, so a job that asks
	 * for Temurin in one of these repositories gets GraalVM and spends minutes
	 * installing a compiler it never runs.
	 */
	public static boolean buildsOnGraalVm(ChoreContext context) {
		var toolVersions = context.resolve(".tool-versions");
		return FilesSilent.exists(toolVersions)
				&& FilesSilent.readString(toolVersions).contains("graalvm");
	}

}
