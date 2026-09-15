package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MavenPlugins;
import io.github.arlol.chorito.tools.MavenPomFile;
import io.github.arlol.chorito.tools.MavenVersions;

/**
 * Keeps the floor the enforcer puts under Maven itself at the release chorito
 * builds on, so a repository that has asked for the rule enforces a version
 * that is still supported rather than whichever one was current when its pom
 * was written.
 * <p>
 * Unlike the formatter, spotbugs and modernizer chores this one never inserts
 * the plugin. The enforcer block carries rules and a third-party rule
 * dependency that are the repository's decision, so a pom that does not run the
 * enforcer, or runs it without {@code requireMavenVersion}, is left alone. What
 * chorito owns is the version, once the repository has asked for the rule.
 */
public class EnforcerPluginChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.rootMavenPomsWithCode(context).forEach(pomXml -> {
			MavenPomFile pom = MavenPomFile.read(pomXml);

			pom.setConfiguration(
					MavenPlugins.ENFORCER,
					MavenVersions.MAVEN,
					"rules",
					"requireMavenVersion",
					"version"
			);

			FilesSilent.writeString(pomXml, pom.asString());
		});
		return context;
	}

}
