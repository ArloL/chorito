package io.github.arlol.chorito.chores;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JavaDirectoryStream;
import io.github.arlol.chorito.tools.JsoupSilent;

public class SpotbugsPluginChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		JavaDirectoryStream.mavenPomsWithSourceCode(context).forEach(pomXml -> {
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
						"\n			<plugin>\n"
								+ "				<groupId>com.github.spotbugs</groupId>\n"
								+ "				<artifactId>spotbugs-maven-plugin</artifactId>\n"
								+ "				<version>4.8.2.0</version>\n"
								+ "				<configuration>\n"
								+ "					<effort>Max</effort>\n"
								+ "					<threshold>Low</threshold>\n"
								+ "				</configuration>\n"
								+ "				<executions>\n"
								+ "					<execution>\n"
								+ "						<goals>\n"
								+ "							<goal>check</goal>\n"
								+ "						</goals>\n"
								+ "					</execution>\n"
								+ "				</executions>\n"
								+ "			</plugin>"
				);
			}

			FilesSilent.writeString(pomXml, doc.outerHtml());
		});
		return context;
	}

}
