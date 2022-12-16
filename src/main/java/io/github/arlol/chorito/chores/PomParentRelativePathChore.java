package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class PomParentRelativePathChore {

	private ChoreContext context;

	public PomParentRelativePathChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path pom = context.resolve("pom.xml");
		if (FilesSilent.exists(pom)) {
			Document doc = JsoupSilent
					.parse(pom, "UTF-8", "", Parser.xmlParser());
			Element parent = doc.selectFirst("project > parent");
			if (parent != null) {
				Element relativePath = parent.selectFirst("relativePath");
				if (relativePath == null) {
					parent.append("<relativePath />\n");
				}
			}
			FilesSilent.writeString(pom, doc.outerHtml());
		}
	}

}
