package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.JsoupSilent;

public class PomPropertiesChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.mavenPoms(context)
				.forEach(PomPropertiesChore::updatePom);
		return context;
	}

	private static void updatePom(Path pom) {
		Document doc = JsoupSilent.parse(pom, "UTF-8", "", Parser.xmlParser());
		renameMainClassPropertyToStartClass(doc);
		if (hasSpringBootParent(doc)) {
			doc.select("artifactId:containsWholeOwnText(maven-jar-plugin)")
					.forEach(
							PomPropertiesChore::removeRedundantManifestMainClass
					);
		}
		String content = doc.outerHtml()
				.replace("${mainClass}", "${start-class}");
		FilesSilent.writeString(pom, content);
	}

	private static void renameMainClassPropertyToStartClass(Document doc) {
		Element properties = doc.selectFirst("project > properties");
		if (properties == null) {
			return;
		}
		Element mainClass = properties.selectFirst("mainClass");
		if (mainClass != null) {
			mainClass.tagName("start-class");
		}
	}

	private static boolean hasSpringBootParent(Document doc) {
		return doc.selectFirst(
				"project > parent > artifactId:containsWholeOwnText(spring-boot-starter-parent)"
		) != null;
	}

	/**
	 * The spring-boot parent already points the jar manifest at the start
	 * class, so a maven-jar-plugin manifest that only repeats the placeholder
	 * is dropped, along with any element it leaves empty.
	 */
	private static void removeRedundantManifestMainClass(Element artifactId) {
		Element plugin = artifactId.parent();
		if (plugin == null) {
			return;
		}
		Element configuration = plugin.selectFirst("configuration");
		if (configuration == null) {
			return;
		}
		Element archive = configuration.selectFirst("archive");
		if (archive == null) {
			return;
		}
		Element manifest = archive.selectFirst("manifest");
		if (manifest == null) {
			return;
		}
		Element mainClass = manifest.selectFirst("mainClass");
		if (mainClass == null || !isStartClassPlaceholder(mainClass.text())) {
			return;
		}
		mainClass.remove();
		if (manifest.childrenSize() == 0) {
			manifest.remove();
		}
		if (archive.childrenSize() == 0) {
			archive.remove();
		}
		if (configuration.childrenSize() == 0) {
			plugin.remove();
		}
	}

	private static boolean isStartClassPlaceholder(String text) {
		return "${start-class}".equals(text) || "${mainClass}".equals(text);
	}

}
