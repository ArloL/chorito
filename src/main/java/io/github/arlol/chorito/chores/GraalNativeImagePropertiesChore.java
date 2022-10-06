package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class GraalNativeImagePropertiesChore {

	private final ChoreContext context;

	public GraalNativeImagePropertiesChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		FilesSilent.walk(
				context.resolve("src/main/resources/META-INF/native-image/")
		).filter(p -> p.endsWith("native-image.properties")).forEach(p -> {
			String content = FilesSilent.readString(p);
			content = content.replace("--allow-incomplete-classpath", "");
			content = content.replace("\n \\\n", "\n");
			FilesSilent.writeString(p, content);
		});
	}

}
