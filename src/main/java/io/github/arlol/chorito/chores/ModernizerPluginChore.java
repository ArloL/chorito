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
		DirectoryStreams.mavenPomsWithCode(context).forEach(pomXml -> {
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
						"""

											<plugin>
												<groupId>org.gaul</groupId>
												<artifactId>modernizer-maven-plugin</artifactId>
												<version>2.9.0</version>
												<configuration>
													<javaVersion>${java.version}</javaVersion>
												</configuration>
												<executions>
													<execution>
														<goals>
															<goal>modernizer</goal>
														</goals>
													</execution>
												</executions>
											</plugin>\
								"""
				);
			}

			FilesSilent.writeString(pomXml, doc.outerHtml());
		});
		return context;
	}

}
