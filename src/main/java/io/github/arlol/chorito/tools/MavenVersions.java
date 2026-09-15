package io.github.arlol.chorito.tools;

/**
 * The Maven release chorito's managed poms require.
 * <p>
 * Like the constants in {@link MavenPluginVersions} this is a deliberate
 * integration point: Renovate keeps it current through a custom manager in
 * chorito's own renovate.json5 that matches this bare string literal. Inlining
 * it into the XML a chore assembles, moving it to another file, or building it
 * from parts does not fail the build -- Renovate simply stops finding it and
 * every managed repository keeps enforcing a floor that is years old.
 */
public abstract class MavenVersions {

	public static final String MAVEN = "3.9.16";

	private MavenVersions() {
	}

}
