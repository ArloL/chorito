package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MavenPlugins;
import io.github.arlol.chorito.tools.MavenPomFile;

public class MavenJavadocSourcesPluginChore implements Chore {

	private static final String PLUGINS = "\n" + "\t\t\t<plugin>\n"
			+ "\t\t\t\t<groupId>org.apache.maven.plugins</groupId>\n"
			+ "\t\t\t\t<artifactId>maven-source-plugin</artifactId>\n"
			+ "\t\t\t\t<executions>\n" + "\t\t\t\t\t<execution>\n"
			+ "\t\t\t\t\t\t<id>attach-sources</id>\n" + "\t\t\t\t\t\t<goals>\n"
			+ "\t\t\t\t\t\t\t<goal>jar-no-fork</goal>\n"
			+ "\t\t\t\t\t\t</goals>\n" + "\t\t\t\t\t</execution>\n"
			+ "\t\t\t\t</executions>\n" + "\t\t\t</plugin>\n"
			+ "\t\t\t<plugin>\n"
			+ "\t\t\t\t<groupId>org.apache.maven.plugins</groupId>\n"
			+ "\t\t\t\t<artifactId>maven-javadoc-plugin</artifactId>\n"
			+ "\t\t\t\t<configuration>\n"
			+ "\t\t\t\t\t<doclint>-missing</doclint>\n"
			+ "\t\t\t\t</configuration>\n" + "\t\t\t\t<executions>\n"
			+ "\t\t\t\t\t<execution>\n"
			+ "\t\t\t\t\t\t<id>attach-javadocs</id>\n" + "\t\t\t\t\t\t<goals>\n"
			+ "\t\t\t\t\t\t\t<goal>jar</goal>\n" + "\t\t\t\t\t\t</goals>\n"
			+ "\t\t\t\t\t</execution>\n" + "\t\t\t\t</executions>\n"
			+ "\t\t\t</plugin>";

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.rootMavenPomsWithCode(context).forEach(pomXml -> {
			MavenPomFile pom = MavenPomFile.read(pomXml);

			if (!pom.hasPlugin(MavenPlugins.MAVEN_SOURCE)) {
				pom.insertPluginAfter(MavenPlugins.MODERNIZER, PLUGINS);
			}

			FilesSilent.writeString(pomXml, pom.asString());
		});
		return context;
	}

}
