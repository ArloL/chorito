package io.github.arlol.chorito.chores;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class PomParentRelativePathChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.mavenPoms(context).forEach(pom -> {
			Document doc = JsoupSilent
					.parse(pom, "UTF-8", "", Parser.xmlParser());
			Element parent = doc.selectFirst("project > parent");
			if (parent != null) {
				Element relativePath = parent.selectFirst("relativePath");
				if (relativePath == null) {
					parent.append("\t<relativePath />\n\t");
				}
			}
			FilesSilent.writeString(pom, doc.outerHtml());
		});
		return context;
	}

}
