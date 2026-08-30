package io.github.arlol.chorito.chores;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class EclipseFormatterPluginChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.rootMavenPomsWithCode(context).forEach(pomXml -> {
			Document doc = JsoupSilent
					.parse(pomXml, "UTF-8", "", Parser.xmlParser());

			Element formatterPlugin = doc.select(
					"plugin:has(groupId:containsWholeOwnText(net.revelc.code.formatter)):has(artifactId:containsWholeOwnText(formatter-maven-plugin))"
			).first();
			if (formatterPlugin == null) {
				Element flattenPlugin = doc.select(
						"plugin:has(groupId:containsWholeOwnText(org.codehaus.mojo)):has(artifactId:containsWholeOwnText(flatten-maven-plugin))"
				).first();
				if (flattenPlugin == null) {
					flattenPlugin = doc.select("plugin").last();
					if (flattenPlugin == null) {
						throw new IllegalStateException("No plugin");
					}
				}
				flattenPlugin.after(
						"\n\t\t\t<plugin>\n"
								+ "\t\t\t\t<groupId>net.revelc.code.formatter</groupId>\n"
								+ "\t\t\t\t<artifactId>formatter-maven-plugin</artifactId>\n"
								+ "\t\t\t\t<version>2.24.1</version>\n"
								+ "\t\t\t\t<configuration>\n"
								+ "\t\t\t\t\t<configFile>${project.basedir}/.settings/code-formatter-profile.xml</configFile>\n"
								+ "\t\t\t\t</configuration>\n"
								+ "\t\t\t\t<executions>\n"
								+ "\t\t\t\t\t<execution>\n"
								+ "\t\t\t\t\t\t<goals>\n"
								+ "\t\t\t\t\t\t\t<goal>format</goal>\n"
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
