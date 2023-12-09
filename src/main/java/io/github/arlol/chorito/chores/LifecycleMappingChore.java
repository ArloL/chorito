package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class LifecycleMappingChore implements Chore {

	@Override
	public void doit(ChoreContext context) {
		Path pomXml = context.resolve("pom.xml");
		if (FilesSilent.exists(pomXml)) {
			Document doc = JsoupSilent
					.parse(pomXml, "UTF-8", "", Parser.xmlParser());

			Element lifecycleMappingPlugin = doc.select(
					"plugin:has(groupId:containsWholeOwnText(org.eclipse.m2e)):has(artifactId:containsWholeOwnText(lifecycle-mapping))"
			).first();
			while (!(lifecycleMappingPlugin
					.previousSibling() instanceof Element)) {
				lifecycleMappingPlugin.previousSibling().remove();
			}
			lifecycleMappingPlugin.remove();

			FilesSilent.writeString(pomXml, doc.outerHtml());
		}
	}

}
