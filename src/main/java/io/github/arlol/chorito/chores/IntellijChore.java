package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class IntellijChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		boolean hasPom = context.textFiles()
				.stream()
				.anyMatch(file -> file.endsWith("pom.xml"));
		boolean hasMvnw = context.textFiles()
				.stream()
				.anyMatch(file -> file.endsWith("mvnw"));
		boolean hasGradlew = context.textFiles()
				.stream()
				.anyMatch(file -> file.endsWith("gradlew"));
		if (hasPom || hasMvnw || hasGradlew) {
			overwriteFromTemplate(context, "eclipseCodeFormatter");
			overwriteFromTemplate(context, "externalDependencies");
			overwriteFromTemplate(context, "saveactions_settings");
			overwriteFromTemplate(context, "codeStyles/codeStyleConfig");
			overwriteProjectFromTemplate(context);
		}
		return context;
	}

	private void overwriteFromTemplate(ChoreContext context, String name) {
		Path path = context.resolve(".idea/" + name + ".xml");
		String template = ClassPathFiles
				.readString("idea-settings/" + name + ".xml");
		if (!FilesSilent.exists(path)) {
			FilesSilent.writeString(path, template);
			context.setDirty();
			return;
		}
	}

	private void overwriteProjectFromTemplate(ChoreContext context) {
		String fileName = "codeStyles/Project";
		Path path = context.resolve(".idea/" + fileName + ".xml");
		String templateString = ClassPathFiles
				.readString("idea-settings/" + fileName + ".xml");

		if (!FilesSilent.exists(path)) {
			FilesSilent.writeString(path, templateString);
			context.setDirty();
			return;
		}

		Document document = JsoupSilent
				.parse(path, null, "", Parser.xmlParser());
		document.outputSettings().prettyPrint(true).indentAmount(2);
		String before = document.outerHtml();

		Element javaCodeStyleSettings = document
				.selectFirst("JavaCodeStyleSettings");

		if (javaCodeStyleSettings == null) {
			FilesSilent.writeString(path, templateString);
			return;
		}

		Document template = Jsoup.parse(templateString, "", Parser.xmlParser());

		for (Element templateOption : template
				.select("JavaCodeStyleSettings > option")) {
			Element documentOption = document.selectFirst(
					"JavaCodeStyleSettings > option[name="
							+ templateOption.attr("name") + "]"
			);
			if (documentOption == null) {
				javaCodeStyleSettings.append(templateOption.outerHtml());
			}
		}

		String after = document.outerHtml();
		if (!after.contentEquals(before)) {
			Elements codeScheme = document.select("component > code_scheme");
			long version = Long.parseLong(codeScheme.attr("version")) + 1;
			codeScheme.attr("version", "" + version);
			FilesSilent.writeString(path, document.outerHtml());
		}
	}

}
