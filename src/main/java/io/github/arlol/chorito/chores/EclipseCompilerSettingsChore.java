package io.github.arlol.chorito.chores;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;
import io.github.arlol.chorito.tools.PropertiesSilent;

public class EclipseCompilerSettingsChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		context.textFiles()
				.stream()
				.filter(file -> file.endsWith("pom.xml"))
				.map(MyPaths::getParent)
				.forEach(pomDir -> {
					String templateJdtCorePrefs = ClassPathFiles.readString(
							"eclipse-settings/org.eclipse.jdt.core.prefs"
					);

					Path jdtCorePrefs = pomDir
							.resolve(".settings/org.eclipse.jdt.core.prefs");
					if (!FilesSilent.exists(jdtCorePrefs)) {
						FilesSilent.touch(jdtCorePrefs);
					}

					Map<String, String> jdtCorePrefsMap = new PropertiesSilent()
							.load(jdtCorePrefs)
							.toMap();
					int prefsHashCodeAtStart = jdtCorePrefsMap.hashCode();

					jdtCorePrefsMap.put("eclipse.preferences.version", "1");

					new PropertiesSilent()
							.load(new StringReader(templateJdtCorePrefs))
							.toMap()
							.entrySet()
							.stream()
							.filter(e -> {
								return e.getKey()
										.startsWith(
												"org.eclipse.jdt.core.compiler."
										)
										|| e.getKey()
												.startsWith(
														"org.eclipse.jdt.core.builder."
												);
							})
							.forEach(
									e -> jdtCorePrefsMap
											.put(e.getKey(), e.getValue())
							);

					// update the file only if something changed
					// otherwise this triggers a rebuild in Eclipse and
					// Eclipse-based tools (VS Code)
					if (jdtCorePrefsMap.hashCode() != prefsHashCodeAtStart) {

						List<String> propertyList = jdtCorePrefsMap.entrySet()
								.stream()
								.map(
										e -> e.getKey() + "="
												+ e.getValue()
														.replace(":", "\\:")
								)
								.toList();

						FilesSilent.write(jdtCorePrefs, propertyList, "\n");
					}
				});
		return context;
	}

}
