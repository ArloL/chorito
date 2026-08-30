package io.github.arlol.chorito.chores;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class ModernizerPluginChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.rootMavenPomsWithCode(context).forEach(pomXml -> {
			Document doc = JsoupSilent
					.parse(pomXml, "UTF-8", "", Parser.xmlParser());

			Element modernizerPlugin = doc.select(
					"plugin:has(groupId:containsWholeOwnText(org.gaul)):has(artifactId:containsWholeOwnText(modernizer-maven-plugin))"
			).first();
			if (modernizerPlugin == null) {
				Element spotbugsPlugin = doc.select(
						"plugin:has(groupId:containsWholeOwnText(com.github.spotbugs)):has(artifactId:containsWholeOwnText(spotbugs-maven-plugin))"
				).first();
				if (spotbugsPlugin == null) {
					throw new IllegalStateException("No spotbugs plugin");
				}
				spotbugsPlugin.after(
						"\n" + "\t\t\t<plugin>\n"
								+ "\t\t\t\t<groupId>org.gaul</groupId>\n"
								+ "\t\t\t\t<artifactId>modernizer-maven-plugin</artifactId>\n"
								+ "\t\t\t\t<version>3.5.0</version>\n"
								+ "\t\t\t\t<configuration>\n"
								+ "\t\t\t\t\t<javaVersion>${java.version}</javaVersion>\n"
								+ "\t\t\t\t</configuration>\n"
								+ "\t\t\t\t<executions>\n"
								+ "\t\t\t\t\t<execution>\n"
								+ "\t\t\t\t\t\t<goals>\n"
								+ "\t\t\t\t\t\t\t<goal>modernizer</goal>\n"
								+ "\t\t\t\t\t\t</goals>\n"
								+ "\t\t\t\t\t</execution>\n"
								+ "\t\t\t\t</executions>\n" + "\t\t\t</plugin>"
				);
			}

			FilesSilent.writeString(pomXml, doc.outerHtml());
		});
		return context;
	}

}
