package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JavaVersions;
import io.github.arlol.chorito.tools.MavenPlugins;
import io.github.arlol.chorito.tools.MavenPomFile;

/**
 * Lets a CI job that builds no native image switch the {@code graal} profile
 * off by setting {@code SKIP_GRAALVM_NATIVE_IMAGE_BUILD}. The profile stays
 * active on a GraalVM JDK, because Maven requires both activation conditions.
 * Every profile containing {@code native-maven-plugin} gets exactly that
 * activation, whatever it had, and a lone one is named {@code graal}, the id
 * the platform jobs pass to {@code -P}: a variant left in place would keep
 * building native images in jobs meant to skip them.
 */
public class GraalNativeProfileSkipChore implements Chore {

	static final String PROFILE_ID = "graal";
	static final String GRAAL_DIRECTORY = "${env.JAVA_HOME}/lib/graal";

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.mavenPoms(context).forEach(pomXml -> {
			MavenPomFile pom = MavenPomFile.read(pomXml);
			if (pom.setNativeProfile(
					MavenPlugins.NATIVE,
					PROFILE_ID,
					GRAAL_DIRECTORY,
					"!env." + JavaVersions.SKIP_NATIVE_IMAGE_ENV
			)) {
				FilesSilent.writeString(pomXml, pom.asString());
			}
		});
		return context;
	}

}
