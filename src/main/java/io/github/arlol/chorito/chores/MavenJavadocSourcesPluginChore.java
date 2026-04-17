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
						"""

											<plugin>
												<groupId>org.apache.maven.plugins</groupId>
												<artifactId>maven-source-plugin</artifactId>
												<executions>
													<execution>
														<id>attach-sources</id>
														<goals>
															<goal>jar-no-fork</goal>
														</goals>
													</execution>
												</executions>
											</plugin>
											<plugin>
												<groupId>org.apache.maven.plugins</groupId>
												<artifactId>maven-javadoc-plugin</artifactId>
												<configuration>
													<doclint>-missing</doclint>
												</configuration>
												<executions>
													<execution>
														<id>attach-javadocs</id>
														<goals>
															<goal>jar</goal>
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
