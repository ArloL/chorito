package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class GraalNativeImageMavenPluginMigrationChore {

	private final ChoreContext context;

	public GraalNativeImageMavenPluginMigrationChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path pomXml = context.resolve("pom.xml");
		if (FilesSilent.exists(pomXml)) {
			Document doc = JsoupSilent
					.parse(pomXml, "UTF-8", "", Parser.xmlParser());
			doc.select("groupId:containsWholeOwnText(org.graalvm.nativeimage)")
					.forEach(groupIdElement -> {
						Element pluginElement = groupIdElement.parent();
						if (pluginElement != null) {
							Element artifactId = pluginElement
									.selectFirst("artifactId");
							if (artifactId != null && artifactId.text()
									.equals("native-image-maven-plugin")) {

								groupIdElement.text("org.graalvm.buildtools");

								artifactId.text("native-maven-plugin");
								Element version = pluginElement
										.selectFirst("version");
								if (version != null) {
									version.text("0.9.14");
								}
								pluginElement.select(
										"goal:containsWholeOwnText(native-image)"
								).forEach(g -> g.text("compile-no-fork"));
							}
						}
					});
			FilesSilent.writeString(pomXml, doc.outerHtml());
		}
	}

}
