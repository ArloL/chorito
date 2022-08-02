package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class DeleteUnnecessaryFilesChore {

	private final ChoreContext context;

	public DeleteUnnecessaryFilesChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path mavenSettings = context
				.resolve(".github/github-packages-maven-settings.xml");
		FilesSilent.deleteIfExists(mavenSettings);
		Path mavenWindowsSettings = context
				.resolve(".github/github-actions-windows-maven-settings.xml");
		FilesSilent.deleteIfExists(mavenWindowsSettings);
	}

}
