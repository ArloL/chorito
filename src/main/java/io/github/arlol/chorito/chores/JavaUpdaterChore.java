package io.github.arlol.chorito.chores;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
		updatePomXmlJavaVersionProperty();
		updateGithubActions();
	}

	private void updatePomXmlJavaVersionProperty() {
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

	private void updateGithubActions() {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				return path.toString().endsWith(".yaml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			List<String> updated = FilesSilent.readAllLines(path)
					.stream()
					.map(s -> {
						if (s.trim().startsWith("JAVA_VERSION: 11")) {
							return s.replace("11", "17");
						}
						return s;
					})
					.toList();
			FilesSilent.write(path, updated, "\n");
		});

	}

}
