package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JavaVersions;
import io.github.arlol.chorito.tools.MavenPomFile;

/**
 * Lets a CI job that builds no native image switch the {@code graal} profile
 * off by setting {@code SKIP_GRAALVM_NATIVE_IMAGE_BUILD}. The profile stays
 * active on a GraalVM JDK, because Maven requires both activation conditions. A
 * profile that checks {@code GRAALVM-README.md} moves to {@code lib/graal}:
 * mise's GraalVM ships the README too, so the profile would never switch off.
 */
public class GraalNativeProfileSkipChore implements Chore {

	static final String GRAAL_DIRECTORY = "lib/graal";
	static final String README = "GRAALVM-README.md";

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.mavenPoms(context).forEach(pomXml -> {
			MavenPomFile pom = MavenPomFile.read(pomXml);
			boolean moved = pom.replaceProfileActivationFileSuffix(
					README,
					GRAAL_DIRECTORY
			);
			boolean added = pom.addProfileActivationProperty(
					GRAAL_DIRECTORY,
					"!env." + JavaVersions.SKIP_NATIVE_IMAGE_ENV
			);
			if (moved || added) {
				FilesSilent.writeString(pomXml, pom.asString());
			}
		});
		return context;
	}

}
