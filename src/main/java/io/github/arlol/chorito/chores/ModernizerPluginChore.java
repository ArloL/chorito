package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class ModernizerPluginChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path pomXml = context.resolve("pom.xml");
		if (FilesSilent.exists(pomXml)) {
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
						"\n" + "			<plugin>\n"
								+ "				<groupId>org.gaul</groupId>\n"
								+ "				<artifactId>modernizer-maven-plugin</artifactId>\n"
								+ "				<version>2.7.0</version>\n"
								+ "				<configuration>\n"
								+ "					<javaVersion>${java.version}</javaVersion>\n"
								+ "				</configuration>\n"
								+ "				<executions>\n"
								+ "					<execution>\n"
								+ "						<goals>\n"
								+ "							<goal>modernizer</goal>\n"
								+ "						</goals>\n"
								+ "					</execution>\n"
								+ "				</executions>\n"
								+ "			</plugin>"
				);
			}

			FilesSilent.writeString(pomXml, doc.outerHtml());
		}
		return context;
	}

}
