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
		DirectoryStreams.mavenPomsWithCode(context).forEach(pomXml -> {
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
						"""

											<plugin>
												<groupId>com.github.spotbugs</groupId>
												<artifactId>spotbugs-maven-plugin</artifactId>
												<version>4.8.6.6</version>
												<configuration>
													<effort>Max</effort>
													<threshold>Low</threshold>
												</configuration>
												<executions>
													<execution>
														<goals>
															<goal>check</goal>
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
