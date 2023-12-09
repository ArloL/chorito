package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class CodeFormatterProfileChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Path pom = context.resolve("pom.xml");
		if (FilesSilent.exists(pom)) {
			Path profileXml = context
					.resolve(".settings/code-formatter-profile.xml");
			String currentProfile = ClassPathFiles
					.readString("/code-formatter-profile.xml");
			FilesSilent.writeString(profileXml, currentProfile);

			Path jdtCorePrefs = context
					.resolve(".settings/org.eclipse.jdt.core.prefs");

			List<String> jdtCorePrefsLines;
			if (FilesSilent.exists(jdtCorePrefs)) {
				jdtCorePrefsLines = new ArrayList<>(
						FilesSilent.readAllLines(jdtCorePrefs)
								.stream()
								.skip(1)
								.filter(
										s -> !s.startsWith(
												"org.eclipse.jdt.core.formatter"
										)
								)
								.toList()
				);
			} else {
				jdtCorePrefsLines = new ArrayList<>(
						List.of(
								"org.eclipse.jdt.core.javaFormatter=org.eclipse.jdt.core.defaultJavaFormatter"
						)
				);
			}

			Document doc = JsoupSilent
					.parse(profileXml, "UTF-8", "", Parser.xmlParser());
			doc.select("setting").forEach(element -> {
				jdtCorePrefsLines
						.add(
								element.attr("id") + "="
										+ element.attr("value")
												.replace(":", "\\:")
						);
			});
			Collections.sort(jdtCorePrefsLines);
			jdtCorePrefsLines.add(0, "eclipse.preferences.version=1");
			FilesSilent.write(jdtCorePrefs, jdtCorePrefsLines, "\n");

			Path jdtUiPrefs = context
					.resolve(".settings/org.eclipse.jdt.ui.prefs");
			if (!FilesSilent.exists(jdtUiPrefs)) {
				String currentJdtUiPrefs = ClassPathFiles
						.readString("/org.eclipse.jdt.ui.prefs");
				FilesSilent.writeString(jdtUiPrefs, currentJdtUiPrefs);
			}
		}
		return context;
	}

}
