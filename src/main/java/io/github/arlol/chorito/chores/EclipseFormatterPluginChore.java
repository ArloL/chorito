package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class EclipseFormatterPluginChore implements Chore {

	@Override
	public void doit(ChoreContext context) {
		Path pomXml = context.resolve("pom.xml");
		if (FilesSilent.exists(pomXml)) {
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
					throw new IllegalStateException("No flatten plugin");
				}
				flattenPlugin.after(
						"\n			<plugin>\n"
								+ "				<groupId>net.revelc.code.formatter</groupId>\n"
								+ "				<artifactId>formatter-maven-plugin</artifactId>\n"
								+ "				<version>2.23.0</version>\n"
								+ "				<configuration>\n"
								+ "					<configFile>${project.basedir}/.settings/code-formatter-profile.xml</configFile>\n"
								+ "				</configuration>\n"
								+ "				<executions>\n"
								+ "					<execution>\n"
								+ "						<goals>\n"
								+ "							<goal>format</goal>\n"
								+ "						</goals>\n"
								+ "					</execution>\n"
								+ "				</executions>\n"
								+ "			</plugin>"
				);
			}

			FilesSilent.writeString(pomXml, doc.outerHtml());
		}
	}

}
