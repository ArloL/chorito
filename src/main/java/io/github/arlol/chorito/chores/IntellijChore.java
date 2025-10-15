package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class IntellijChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.javaDirs(context).forEach(dir -> {
			writeFromTemplate(context, dir, "eclipseCodeFormatter");
			writeFromTemplate(context, dir, "saveactions_settings");
			writeFromTemplate(context, dir, "codeStyles/codeStyleConfig");
			overwriteExternalDependenciesXmlFromTemplate(context, dir);
			overwriteProjectXmlFromTemplate(context, dir);
		});
		return context;
	}

	private void writeFromTemplate(
			ChoreContext context,
			Path dir,
			String name
	) {
		Path path = dir.resolve(".idea/" + name + ".xml");
		String template = ClassPathFiles
				.readString("idea-settings/" + name + ".xml");
		if (!FilesSilent.exists(path)) {
			FilesSilent.writeString(path, template);
			context.setDirty();
			return;
		}
	}

	private void overwriteExternalDependenciesXmlFromTemplate(
			ChoreContext context,
			Path dir
	) {
		String name = "externalDependencies";
		Path path = dir.resolve(".idea/" + name + ".xml");
		String templateString = ClassPathFiles
				.readString("idea-settings/" + name + ".xml");
		if (!FilesSilent.exists(path)) {
			FilesSilent.writeString(path, templateString);
			context.setDirty();
			return;
		}

		Document document = JsoupSilent
				.parse(path, null, "", Parser.xmlParser());
		document.outputSettings()
				.prettyPrint(true)
				.indentAmount(2)
				.outline(true);
		String before = document.outerHtml();

		Element externalDependencies = document
				.selectFirst("component[name=ExternalDependencies]");

		if (externalDependencies == null) {
			FilesSilent.writeString(path, templateString);
			return;
		}

		Document template = Jsoup.parse(templateString, "", Parser.xmlParser());

		for (Element templatePlugin : template
				.select("component[name=ExternalDependencies] > plugin")) {
			Element documentPlugin = document.selectFirst(
					"component[name=ExternalDependencies] > plugin[id="
							+ templatePlugin.attr("id") + "]"
			);
			if (documentPlugin == null) {
				externalDependencies.append(templatePlugin.outerHtml());
			} else {
				for (var attribute : templatePlugin.attributes()) {
					documentPlugin
							.attr(attribute.getKey(), attribute.getValue());
				}
			}
		}

		String after = document.outerHtml();
		if (!after.contentEquals(before)) {
			FilesSilent.writeString(path, document.outerHtml());
		}
	}

	private void overwriteProjectXmlFromTemplate(
			ChoreContext context,
			Path dir
	) {
		String fileName = "codeStyles/Project";
		Path path = dir.resolve(".idea/" + fileName + ".xml");
		String templateString = ClassPathFiles
				.readString("idea-settings/" + fileName + ".xml");

		if (!FilesSilent.exists(path)) {
			FilesSilent.writeString(path, templateString);
			context.setDirty();
			return;
		}

		Document document = JsoupSilent
				.parse(path, null, "", Parser.xmlParser());
		document.outputSettings()
				.prettyPrint(true)
				.indentAmount(2)
				.outline(true);
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
