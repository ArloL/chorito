package io.github.arlol.chorito.tools;

public abstract class JavaVersions {

	/**
	 * The Temurin release chorito pins in {@code .tool-versions}. Renovate
	 * keeps it current through the custom manager in chorito's own
	 * renovate.json5.
	 */
	public static final String TEMURIN = "25.0.5+8.0.LTS";

	/**
	 * The GraalVM community innovation release chorito pins in
	 * {@code .tool-versions}. Renovate keeps it current through the custom
	 * manager in chorito's own renovate.json5.
	 */
	public static final String GRAALVM = "25.0.5.0.3";

	private JavaVersions() {
	}

	/**
	 * Whether {@code .tool-versions} selects a GraalVM. It is the one place
	 * that says so: a migrated workflow installs its JDK through mise and no
	 * longer mentions GraalVM.
	 */
	public static boolean buildsOnGraalVm(ChoreContext context) {
		var toolVersions = context.resolve(".tool-versions");
		return FilesSilent.exists(toolVersions)
				&& FilesSilent.readString(toolVersions).contains("graalvm");
	}

}
