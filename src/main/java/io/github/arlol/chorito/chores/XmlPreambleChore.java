package io.github.arlol.chorito.chores;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class XmlPreambleChore implements Chore {

	@Override
	public void doit(ChoreContext context) {
		context.textFiles()
				.stream()
				.filter(path -> path.toString().endsWith(".xml"))
				.forEach(path -> {
					Document doc = JsoupSilent
							.parse(path, null, "", Parser.xmlParser());
					doc.charset(doc.outputSettings().charset());
					Node xmlDeclaration = doc.firstChild();
					if (xmlDeclaration != null) {
						Node nextSibling = xmlDeclaration.nextSibling();
						if (nextSibling != null
								&& !nextSibling.outerHtml().startsWith("\n")) {
							xmlDeclaration.after(new TextNode("\n"));
						}
					}
					FilesSilent.writeString(path, doc.outerHtml());
				});
	}

}
