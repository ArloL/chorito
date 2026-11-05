package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MavenPomFile;

/**
 * Lets a CI job that builds no native image switch the {@code graal} profile
 * off by setting {@code SKIP_GRAALVM_NATIVE_IMAGE_BUILD}. The profile stays
 * active on a GraalVM JDK, because Maven requires both activation conditions.
 */
public class GraalNativeProfileSkipChore implements Chore {

	static final String GRAAL_DIRECTORY = "lib/graal";
	static final String SKIP_VARIABLE = "SKIP_GRAALVM_NATIVE_IMAGE_BUILD";

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.mavenPoms(context).forEach(pomXml -> {
			MavenPomFile pom = MavenPomFile.read(pomXml);
			if (pom.addProfileActivationProperty(
					GRAAL_DIRECTORY,
					"!env." + SKIP_VARIABLE
			)) {
				FilesSilent.writeString(pomXml, pom.asString());
			}
		});
		return context;
	}

}
