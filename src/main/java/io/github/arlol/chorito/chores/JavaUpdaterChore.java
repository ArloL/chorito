package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;
import io.github.arlol.chorito.tools.PropertiesSilent;

public class JavaUpdaterChore implements Chore {

	private static final String OPENJDK_25 = "openjdk25";

	@Override
	public ChoreContext doit(ChoreContext context) {
		updatePomXmlJavaVersionProperty(context);
		updateGithubActions(context);
		updateJitpackYml(context);
		updateEclipseSettings(context);
		return context;
	}

	private void updateEclipseSettings(ChoreContext context) {
		Path prefsFile = context
				.resolve(".settings/org.eclipse.jdt.core.prefs");
		if (!FilesSilent.exists(prefsFile)) {
			return;
		}
		Map<String, String> prefsMap = new PropertiesSilent().load(prefsFile)
				.toMap();
		prefsMap.put("org.eclipse.jdt.core.compiler.source", "25");
		prefsMap.put("org.eclipse.jdt.core.compiler.compliance", "25");
		prefsMap.put(
				"org.eclipse.jdt.core.compiler.codegen.targetPlatform",
				"25"
		);
		List<String> propertyList = prefsMap.entrySet()
				.stream()
				.map(e -> e.getKey() + "=" + e.getValue().replace(":", "\\:"))
				.toList();
		FilesSilent.write(prefsFile, propertyList, "\n");
	}

	private void updatePomXmlJavaVersionProperty(ChoreContext context) {
		DirectoryStreams.rootMavenPomsWithCode(context).forEach(pomXml -> {
			Document doc = JsoupSilent
					.parse(pomXml, "UTF-8", "", Parser.xmlParser());
			Element project = doc.selectFirst("project");
			if (project == null) {
				return;
			}
			Element properties = doc.selectFirst("project > properties");
			if (properties == null) {
				project.append(
						"\t<properties>\n"
								+ "\t\t<java.version>25</java.version>\n"
								+ "\t</properties>\n"
				);
			} else {
				Elements javaVersionElements = properties
						.getElementsByTag("java.version");
				if (javaVersionElements.isEmpty()) {
					properties.append("\t<java.version>25</java.version>\n\t");
				} else {
					javaVersionElements.stream()
							.filter(e -> e.text().equals("11"))
							.forEach(e -> e.text("25"));
					javaVersionElements.stream()
							.filter(e -> e.text().equals("17"))
							.forEach(e -> e.text("25"));
					javaVersionElements.stream()
							.filter(e -> e.text().equals("21"))
							.forEach(e -> e.text("25"));
				}
			}

			FilesSilent.writeString(pomXml, doc.outerHtml());
		});
	}

	private void updateGithubActions(ChoreContext context) {
		Path workflowsLocation = context.resolve(".github/workflows");
		context.textFiles().stream().filter(path -> {
			if (path.startsWith(workflowsLocation)) {
				String fileName = path.toString();
				return fileName.endsWith(".yaml") || fileName.endsWith(".yml");
			}
			return false;
		}).map(context::resolve).forEach(path -> {
			List<String> updated = FilesSilent.readAllLines(path)
					.stream()
					.map(s -> {
						if (s.trim().startsWith("JAVA_VERSION: 11")) {
							return s.replace("11", "25");
						}
						if (s.trim().startsWith("JAVA_VERSION: 17")) {
							return s.replace("17", "25");
						}
						if (s.trim().startsWith("JAVA_VERSION: 21")) {
							return s.replace("21", "25");
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
							return s.replace("openjdk8", OPENJDK_25);
						}
						if (s.trim().equals("- openjdk11")) {
							return s.replace("openjdk11", OPENJDK_25);
						}
						if (s.trim().equals("- openjdk17")) {
							return s.replace("openjdk17", OPENJDK_25);
						}
						if (s.trim().equals("- openjdk21")) {
							return s.replace("openjdk21", OPENJDK_25);
						}
						return s;
					})
					.toList();
			FilesSilent.write(jitpack, updated, "\n");
		}
	}

}
