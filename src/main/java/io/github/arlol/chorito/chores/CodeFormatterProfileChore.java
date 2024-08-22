package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JavaDirectoryStream;
import io.github.arlol.chorito.tools.JsoupSilent;
import io.github.arlol.chorito.tools.PropertiesSilent;

public class CodeFormatterProfileChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		JavaDirectoryStream.mavenPoms(context).forEach(pom -> {
			Path profileXml = context
					.resolve(".settings/code-formatter-profile.xml");
			String currentProfile = ClassPathFiles
					.readString("eclipse-settings/code-formatter-profile.xml");
			FilesSilent.writeString(profileXml, currentProfile);

			Path jdtCorePrefs = context
					.resolve(".settings/org.eclipse.jdt.core.prefs");

			Map<String, String> jdtCorePrefsMap;
			if (FilesSilent.exists(jdtCorePrefs)) {
				jdtCorePrefsMap = new PropertiesSilent().load(jdtCorePrefs)
						.toMap();
			} else {
				jdtCorePrefsMap = new TreeMap<>();
			}
			int prefsHashCodeAtStart = jdtCorePrefsMap.hashCode();

			jdtCorePrefsMap.put("eclipse.preferences.version", "1");
			jdtCorePrefsMap.put(
					"org.eclipse.jdt.core.javaFormatter",
					"org.eclipse.jdt.core.defaultJavaFormatter"
			);

			Document doc = JsoupSilent
					.parse(profileXml, "UTF-8", "", Parser.xmlParser());
			doc.select("setting").forEach(element -> {
				jdtCorePrefsMap.put(element.attr("id"), element.attr("value"));
			});

			List<String> propertyList = jdtCorePrefsMap.entrySet()
					.stream()
					.map(
							e -> e.getKey() + "="
									+ e.getValue().replace(":", "\\:")
					)
					.toList();
			// update the file only if something changed - otherwise this
			// triggers a rebuild in Eclipse and Eclipse-based tools (VS Code)
			if (jdtCorePrefsMap.hashCode() != prefsHashCodeAtStart) {
				FilesSilent.write(jdtCorePrefs, propertyList, "\n");
			}

			Path jdtUiPrefs = context
					.resolve(".settings/org.eclipse.jdt.ui.prefs");
			String currentJdtUiPrefs = ClassPathFiles
					.readString("eclipse-settings/org.eclipse.jdt.ui.prefs");
			if (!FilesSilent.exists(jdtUiPrefs)) {
				FilesSilent.writeString(jdtUiPrefs, currentJdtUiPrefs);
			}
		});
		return context;
	}

}
