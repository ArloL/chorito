package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.List;

public abstract class ExistingFileUpdater {

	private static String OLD_SUFFIX = "# End of chorito. Add your ignores after this line and they will be preserved.";

	private static String NEW_SUFFIX = "# Add custom ignores after this line to be preserved during automated updates";

	private ExistingFileUpdater() {
	}

	public static void update(Path target, String content) {
		content += "\n" + NEW_SUFFIX;
		if (!FilesSilent.exists(target)) {
			FilesSilent.writeString(target, content + "\n");
			return;
		}
		List<String> current = FilesSilent.readAllLines(target);
		int endOfChorito = current.indexOf(OLD_SUFFIX);
		if (endOfChorito == -1) {
			endOfChorito = current.indexOf(NEW_SUFFIX);
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
