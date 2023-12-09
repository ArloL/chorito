package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ClassPathFiles;
import io.github.arlol.chorito.tools.FilesSilent;

public class CodeFormatterProfileChore {

	private final ChoreContext context;

	public CodeFormatterProfileChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path pom = context.resolve("pom.xml");
		if (FilesSilent.exists(pom)) {
			Path profileXml = context
					.resolve(".settings/code-formatter-profile.xml");
			String currentProfile = ClassPathFiles
					.readString("/code-formatter-profile.xml");
			FilesSilent.writeString(profileXml, currentProfile);
		}
	}

}
