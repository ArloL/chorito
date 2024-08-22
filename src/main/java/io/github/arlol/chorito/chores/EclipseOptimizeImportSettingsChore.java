package io.github.arlol.chorito.chores;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JavaDirectoryStream;
import io.github.arlol.chorito.tools.PropertiesSilent;

public class EclipseOptimizeImportSettingsChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		JavaDirectoryStream.javaDirectories(context).forEach(pomDir -> {
			String templateJdtUiPrefs = ClassPathFiles
					.readString("eclipse-settings/org.eclipse.jdt.ui.prefs");

			Path jdtUiPrefs = pomDir
					.resolve(".settings/org.eclipse.jdt.ui.prefs");
			if (!FilesSilent.exists(jdtUiPrefs)) {
				FilesSilent.touch(jdtUiPrefs);
				context.setDirty();
			}

			Map<String, String> jdtUiPrefsMap = new PropertiesSilent()
					.load(jdtUiPrefs)
					.toMap();
			int prefsHashCodeAtStart = jdtUiPrefsMap.hashCode();

			jdtUiPrefsMap.put("eclipse.preferences.version", "1");

			new PropertiesSilent().load(new StringReader(templateJdtUiPrefs))
					.toMap()
					.entrySet()
					.stream()
					.filter(e -> e.getKey().startsWith("org.eclipse.jdt.ui."))
					.forEach(
							e -> jdtUiPrefsMap
									.putIfAbsent(e.getKey(), e.getValue())
					);

			// update the file only if something changed
			// otherwise this triggers a rebuild in Eclipse and
			// Eclipse-based tools (VS Code)
			if (jdtUiPrefsMap.hashCode() != prefsHashCodeAtStart) {

				List<String> propertyList = jdtUiPrefsMap.entrySet()
						.stream()
						.map(
								e -> e.getKey() + "="
										+ e.getValue().replace(":", "\\:")
						)
						.toList();

				FilesSilent.write(jdtUiPrefs, propertyList, "\n");
			}
		});
		return context;
	}

}
