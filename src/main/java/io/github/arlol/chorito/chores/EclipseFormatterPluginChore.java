package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MavenPluginVersions;
import io.github.arlol.chorito.tools.MavenPlugins;
import io.github.arlol.chorito.tools.MavenPomFile;

public class EclipseFormatterPluginChore implements Chore {

	private static final String PLUGIN = "\n\t\t\t<plugin>\n"
			+ "\t\t\t\t<groupId>net.revelc.code.formatter</groupId>\n"
			+ "\t\t\t\t<artifactId>formatter-maven-plugin</artifactId>\n"
			+ "\t\t\t\t<version>" + MavenPluginVersions.FORMATTER
			+ "</version>\n" + "\t\t\t\t<configuration>\n"
			+ "\t\t\t\t\t<configFile>${project.basedir}/.settings/code-formatter-profile.xml</configFile>\n"
			+ "\t\t\t\t</configuration>\n" + "\t\t\t\t<executions>\n"
			+ "\t\t\t\t\t<execution>\n" + "\t\t\t\t\t\t<goals>\n"
			+ "\t\t\t\t\t\t\t<goal>format</goal>\n" + "\t\t\t\t\t\t</goals>\n"
			+ "\t\t\t\t\t</execution>\n" + "\t\t\t\t</executions>\n"
			+ "\t\t\t</plugin>";

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.rootMavenPomsWithCode(context).forEach(pomXml -> {
			MavenPomFile pom = MavenPomFile.read(pomXml);

			if (!pom.hasPlugin(MavenPlugins.FORMATTER)) {
				// Unlike the chores that follow it, this one starts the chain
				// and so has no predecessor to anchor against: it uses the
				// flatten plugin when there is one and whatever plugin comes
				// last otherwise.
				if (pom.hasPlugin(MavenPlugins.FLATTEN)) {
					pom.insertPluginAfter(MavenPlugins.FLATTEN, PLUGIN);
				} else {
					pom.insertPluginAfterLastPlugin(PLUGIN);
				}
			}

			FilesSilent.writeString(pomXml, pom.asString());
		});
		return context;
	}

}
