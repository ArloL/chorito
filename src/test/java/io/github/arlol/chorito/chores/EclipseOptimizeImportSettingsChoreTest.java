package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class EclipseOptimizeImportSettingsChoreTest {

	private static final String EXPECTED_JDT_UI_PREFS = """
			eclipse.preferences.version=1
			org.eclipse.jdt.ui.ignorelowercasenames=true
			org.eclipse.jdt.ui.importorder=java;javax;org;com;
			org.eclipse.jdt.ui.ondemandthreshold=30
			org.eclipse.jdt.ui.staticondemandthreshold=30
			""";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new EclipseOptimizeImportSettingsChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		doit();
	}

	@Test
	public void test() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.touch(pom);

		doit();

		Path jdtUiPrefs = extension.root()
				.resolve(".settings/org.eclipse.jdt.ui.prefs");

		assertThat(jdtUiPrefs).exists();
		assertThat(jdtUiPrefs).isNotEmptyFile();
		assertThat(jdtUiPrefs).content().isEqualTo(EXPECTED_JDT_UI_PREFS);
	}

}
