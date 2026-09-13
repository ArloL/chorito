package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MavenPluginVersions;
import io.github.arlol.chorito.tools.MavenPlugins;
import io.github.arlol.chorito.tools.MavenPomFile;

public class ModernizerPluginChore implements Chore {

	private static final String PLUGIN = "\n" + "\t\t\t<plugin>\n"
			+ "\t\t\t\t<groupId>org.gaul</groupId>\n"
			+ "\t\t\t\t<artifactId>modernizer-maven-plugin</artifactId>\n"
			+ "\t\t\t\t<version>" + MavenPluginVersions.MODERNIZER
			+ "</version>\n" + "\t\t\t\t<configuration>\n"
			+ "\t\t\t\t\t<javaVersion>${java.version}</javaVersion>\n"
			+ "\t\t\t\t</configuration>\n" + "\t\t\t\t<executions>\n"
			+ "\t\t\t\t\t<execution>\n" + "\t\t\t\t\t\t<goals>\n"
			+ "\t\t\t\t\t\t\t<goal>modernizer</goal>\n"
			+ "\t\t\t\t\t\t</goals>\n" + "\t\t\t\t\t</execution>\n"
			+ "\t\t\t\t</executions>\n" + "\t\t\t</plugin>";

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.rootMavenPomsWithCode(context).forEach(pomXml -> {
			MavenPomFile pom = MavenPomFile.read(pomXml);

			if (!pom.hasPlugin(MavenPlugins.MODERNIZER)) {
				pom.insertPluginAfter(MavenPlugins.SPOTBUGS, PLUGIN);
			}

			FilesSilent.writeString(pomXml, pom.asString());
		});
		return context;
	}

}
