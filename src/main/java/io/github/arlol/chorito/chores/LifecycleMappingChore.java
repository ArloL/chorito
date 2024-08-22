package io.github.arlol.chorito.chores;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.JsoupSilent;

public class LifecycleMappingChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.mavenPoms(context).forEach(pomXml -> {
			Document doc = JsoupSilent
					.parse(pomXml, "UTF-8", "", Parser.xmlParser());

			Elements lifecycleMappingPlugins = doc.select(
					"plugin:has(groupId:containsWholeOwnText(org.eclipse.m2e)):has(artifactId:containsWholeOwnText(lifecycle-mapping))"
			);
			for (Element lifecycleMappingPlugin : lifecycleMappingPlugins) {
				while (!(lifecycleMappingPlugin
						.previousSibling() instanceof Element)) {
					Node previousSibling = lifecycleMappingPlugin
							.previousSibling();
					if (previousSibling != null) {
						previousSibling.remove();
					}
				}
				lifecycleMappingPlugin.remove();
			}
			FilesSilent.writeString(pomXml, doc.outerHtml());
		});
		return context;
	}

}
