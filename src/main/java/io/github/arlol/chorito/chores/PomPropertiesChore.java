package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class PomPropertiesChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path pom = context.resolve("pom.xml");
		if (FilesSilent.exists(pom)) {
			Document doc = JsoupSilent
					.parse(pom, "UTF-8", "", Parser.xmlParser());
			Element properties = doc.selectFirst("project > properties");
			if (properties != null) {
				Element mainClass = properties.selectFirst("mainClass");
				if (mainClass != null) {
					mainClass.tagName("start-class");
				}
			}
			Element springBootParent = doc.selectFirst(
					"project > parent > artifactId:containsWholeOwnText(spring-boot-starter-parent)"
			);
			if (springBootParent != null) {
				doc.select("artifactId:containsWholeOwnText(maven-jar-plugin)")
						.forEach(element -> {
							Element plugin = element.parent();
							if (plugin != null) {
								Element configuration = plugin
										.selectFirst("configuration");
								if (configuration != null) {
									Element archive = configuration
											.selectFirst("archive");
									if (archive != null) {
										Element manifest = archive
												.selectFirst("manifest");
										if (manifest != null) {
											Element mainClass = manifest
													.selectFirst("mainClass");
											if (mainClass != null && (mainClass
													.text()
													.equals("${start-class}")
													|| mainClass.text()
															.equals(
																	"${mainClass}"
															))) {
												mainClass.remove();
												if (manifest
														.childrenSize() == 0) {
													manifest.remove();
												}
												if (archive
														.childrenSize() == 0) {
													archive.remove();
												}
												if (configuration
														.childrenSize() == 0) {
													plugin.remove();
												}
											}
										}
									}
								}
							}
						});
			}
			String content = doc.outerHtml();
			content = content.replace("${mainClass}", "${start-class}");
			FilesSilent.writeString(pom, content);
		}
		return context;
	}

}
