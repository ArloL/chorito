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
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.JsoupSilent;

public class IntellijChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.javaDirectories(context).forEach(dir -> {
			overwriteFromTemplate(context, dir, "eclipseCodeFormatter");
			overwriteFromTemplate(context, dir, "externalDependencies");
			overwriteFromTemplate(context, dir, "saveactions_settings");
			overwriteFromTemplate(context, dir, "codeStyles/codeStyleConfig");
			overwriteProjectFromTemplate(context, dir);
		});
		return context;
	}

	private void overwriteFromTemplate(
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

	private void overwriteProjectFromTemplate(ChoreContext context, Path dir) {
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
