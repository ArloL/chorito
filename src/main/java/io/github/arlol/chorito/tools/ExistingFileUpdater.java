package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.List;

public abstract class ExistingFileUpdater {

	private static final List<String> OLD_SUFFIXES = List.of(
			"# End of chorito. Add your ignores after this line and they will be preserved.",
			"# Add custom ignores after this line to be preserved during automated updates",
			"# Add custom attributes after this line to be preserved during automated updates"
	);

	private static final String SUFFIX = "# Add custom entries after this line to be preserved during automated updates";

	private ExistingFileUpdater() {
	}

	public static void update(Path target, String content) {
		content += "\n" + SUFFIX;
		if (!FilesSilent.exists(target)) {
			FilesSilent.writeString(target, content + "\n");
			return;
		}
		List<String> current = FilesSilent.readAllLines(target);
		int endOfChorito = -1;
		for (String oldSuffix : OLD_SUFFIXES) {
			endOfChorito = current.indexOf(oldSuffix);
			if (endOfChorito != -1) {
				break;
			}
		}
		if (endOfChorito == -1) {
			endOfChorito = current.indexOf(SUFFIX);
		}
		if (endOfChorito == -1) {
			FilesSilent.writeString(target, content + "\n");
			return;
		}
		current = current.subList(endOfChorito + 1, current.size());
		current.add(0, content);
		FilesSilent.write(target, current, "\n");
	}

}
