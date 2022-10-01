package io.github.arlol.chorito.chores;

import java.io.IOException;
import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class GraalNativeImageMavenPluginMigrationChore {

	private final ChoreContext context;

	public GraalNativeImageMavenPluginMigrationChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path pomXml = context.resolve("pom.xml");
		if (FilesSilent.exists(pomXml)) {
			try {
				Document doc = Jsoup.parse(
						pomXml.toFile(),
						"UTF-8",
						"",
						Parser.xmlParser()
				);
				doc.select(
						"groupId:containsWholeOwnText(org.graalvm.nativeimage)"
				).forEach(e -> {
					Element plugin = e.parent();
					Element artifactId = plugin.selectFirst("artifactId");
					if (artifactId.text().equals("native-image-maven-plugin")) {
						plugin.selectFirst("groupId")
								.text("org.graalvm.buildtools");
						artifactId.text("native-maven-plugin");
						Element version = plugin.selectFirst("version");
						if (version != null) {
							version.text("0.9.14");
						}
						plugin.select("goal:containsWholeOwnText(native-image)")
								.forEach(g -> g.text("compile-no-fork"));
					}

				});
				FilesSilent.writeString(pomXml, doc.outerHtml());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

}
