package io.github.arlol.chorito.chores;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class SpotbugsPluginChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.rootMavenPomsWithCode(context).forEach(pomXml -> {
			Document doc = JsoupSilent
					.parse(pomXml, "UTF-8", "", Parser.xmlParser());

			Element spotbugsPlugin = doc.select(
					"plugin:has(groupId:containsWholeOwnText(com.github.spotbugs)):has(artifactId:containsWholeOwnText(spotbugs-maven-plugin))"
			).first();
			if (spotbugsPlugin == null) {
				Element formatterPlugin = doc.select(
						"plugin:has(groupId:containsWholeOwnText(net.revelc.code.formatter)):has(artifactId:containsWholeOwnText(formatter-maven-plugin))"
				).first();
				if (formatterPlugin == null) {
					throw new IllegalStateException("No formatter plugin");
				}
				formatterPlugin.after(
						"\n" + "\t\t\t<plugin>\n"
								+ "\t\t\t\t<groupId>com.github.spotbugs</groupId>\n"
								+ "\t\t\t\t<artifactId>spotbugs-maven-plugin</artifactId>\n"
								+ "\t\t\t\t<version>4.10.3.0</version>\n"
								+ "\t\t\t\t<configuration>\n"
								+ "\t\t\t\t\t<effort>Max</effort>\n"
								+ "\t\t\t\t\t<threshold>Low</threshold>\n"
								+ "\t\t\t\t</configuration>\n"
								+ "\t\t\t\t<executions>\n"
								+ "\t\t\t\t\t<execution>\n"
								+ "\t\t\t\t\t\t<goals>\n"
								+ "\t\t\t\t\t\t\t<goal>check</goal>\n"
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
