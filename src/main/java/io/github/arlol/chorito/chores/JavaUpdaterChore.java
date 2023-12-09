package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class JavaUpdaterChore implements Chore {

	@Override
	public void doit(ChoreContext context) {
		updatePomXmlJavaVersionProperty(context);
		updateGithubActions(context);
		updateJitpackYml(context);
	}

	private void updatePomXmlJavaVersionProperty(ChoreContext context) {
		Path pomXml = context.resolve("pom.xml");
		if (FilesSilent.exists(pomXml)) {
			Document doc = JsoupSilent
					.parse(pomXml, "UTF-8", "", Parser.xmlParser());
			doc.getElementsByTag("java.version")
					.stream()
					.filter(e -> e.text().equals("11"))
					.forEach(e -> e.text("17"));
			FilesSilent.writeString(pomXml, doc.outerHtml());
		}
	}

	private void updateGithubActions(ChoreContext context) {
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

	private void updateJitpackYml(ChoreContext context) {
		Path jitpack = context.resolve("jitpack.yml");
		if (FilesSilent.exists(jitpack)) {
			List<String> updated = FilesSilent.readAllLines(jitpack)
					.stream()
					.map(s -> {
						if (s.trim().equals("- openjdk8")) {
							return s.replace("openjdk8", "openjdk17");
						}
						if (s.trim().equals("- openjdk11")) {
							return s.replace("openjdk11", "openjdk17");
						}
						return s;
					})
					.toList();
			FilesSilent.write(jitpack, updated, "\n");
		}
	}

}
