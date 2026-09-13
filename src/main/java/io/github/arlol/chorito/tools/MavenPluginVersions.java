package io.github.arlol.chorito.tools;

/**
 * The plugin versions chorito writes into a managed pom.
 * <p>
 * These are deliberate integration points, not an incidental extraction:
 * Renovate keeps them current through the custom managers in chorito's own
 * renovate.json5, which match one constant each in this file. Inlining one back
 * into the XML a chore assembles, or moving it to another file, does not fail
 * the build -- Renovate simply stops finding the dependency and every managed
 * repository quietly drifts onto an old plugin. Keep them here, keep the name,
 * and keep the value a bare string literal.
 *
 * @see MavenPlugins
 */
public abstract class MavenPluginVersions {

	public static final String FORMATTER = "2.24.1";

	public static final String SPOTBUGS = "4.10.3.0";

	public static final String MODERNIZER = "3.5.0";

	private MavenPluginVersions() {
	}

}
