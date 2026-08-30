package io.github.arlol.chorito.chores;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class MavenJavadocSourcesPluginChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.rootMavenPomsWithCode(context).forEach(pomXml -> {
			Document doc = JsoupSilent
					.parse(pomXml, "UTF-8", "", Parser.xmlParser());

			Element sourcePlugin = doc.select(
					"plugin:has(groupId:containsWholeOwnText(org.apache.maven.plugins)):has(artifactId:containsWholeOwnText(maven-source-plugin))"
			).first();
			if (sourcePlugin == null) {
				Element modernizerPlugin = doc.select(
						"plugin:has(groupId:containsWholeOwnText(org.gaul)):has(artifactId:containsWholeOwnText(modernizer-maven-plugin))"
				).first();
				if (modernizerPlugin == null) {
					throw new IllegalStateException("No modernizer plugin");
				}
				modernizerPlugin.after(
						"\n" + "\t\t\t<plugin>\n"
								+ "\t\t\t\t<groupId>org.apache.maven.plugins</groupId>\n"
								+ "\t\t\t\t<artifactId>maven-source-plugin</artifactId>\n"
								+ "\t\t\t\t<executions>\n"
								+ "\t\t\t\t\t<execution>\n"
								+ "\t\t\t\t\t\t<id>attach-sources</id>\n"
								+ "\t\t\t\t\t\t<goals>\n"
								+ "\t\t\t\t\t\t\t<goal>jar-no-fork</goal>\n"
								+ "\t\t\t\t\t\t</goals>\n"
								+ "\t\t\t\t\t</execution>\n"
								+ "\t\t\t\t</executions>\n"
								+ "\t\t\t</plugin>\n" + "\t\t\t<plugin>\n"
								+ "\t\t\t\t<groupId>org.apache.maven.plugins</groupId>\n"
								+ "\t\t\t\t<artifactId>maven-javadoc-plugin</artifactId>\n"
								+ "\t\t\t\t<configuration>\n"
								+ "\t\t\t\t\t<doclint>-missing</doclint>\n"
								+ "\t\t\t\t</configuration>\n"
								+ "\t\t\t\t<executions>\n"
								+ "\t\t\t\t\t<execution>\n"
								+ "\t\t\t\t\t\t<id>attach-javadocs</id>\n"
								+ "\t\t\t\t\t\t<goals>\n"
								+ "\t\t\t\t\t\t\t<goal>jar</goal>\n"
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
