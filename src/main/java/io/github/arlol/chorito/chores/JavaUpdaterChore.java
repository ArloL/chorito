package io.github.arlol.chorito.chores;

import java.io.IOException;
import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class JavaUpdaterChore {

	private final ChoreContext context;

	public JavaUpdaterChore(ChoreContext context) {
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
				doc.getElementsByTag("java.version")
						.stream()
						.filter(e -> e.text().equals("11"))
						.forEach(e -> e.text("17"));
				FilesSilent.writeString(pomXml, doc.outerHtml());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

}
