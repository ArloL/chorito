package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class IntellijChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private void doit() {
		new IntellijChore().doit(extension.choreContext());
	}

	@Test
	public void testWithNothing() {
		doit();
	}

	@Test
	public void test() throws Exception {
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));

		doit();

		Path codeStyleConfig = extension.root()
				.resolve(".idea/codeStyles/codeStyleConfig.xml");

		assertThat(codeStyleConfig).exists();
		assertThat(codeStyleConfig).isNotEmptyFile();
	}

	@Test
	public void testPreserveCodestyles() throws Exception {

		// given
		Path codeStylesProjectXml = extension.root()
				.resolve(".idea/codeStyles/Project.xml");
		FilesSilent.touch(extension.root().resolve("src/main/java/Main.java"));
		FilesSilent.touch(extension.root().resolve("pom.xml"));
		FilesSilent.writeString(
				codeStylesProjectXml,
				"""
						<?xml version="1.0" encoding="UTF-8"?>
						<component name="ProjectCodeStyleConfiguration">
						  <code_scheme name="Project" version="173">
						    <JavaCodeStyleSettings>
						      <option name="CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND" value="10" />
						      <option name="IMPORT_LAYOUT_TABLE">
						        <value>
						          <package name="" withSubpackages="true" static="false" />
						        </value>
						      </option>
						    </JavaCodeStyleSettings>
						  </code_scheme>
						</component>"""
		);

		doit();

		assertThat(codeStylesProjectXml).content()
				.isEqualTo(
						"""
								<?xml version="1.0" encoding="UTF-8"?>
								<component name="ProjectCodeStyleConfiguration">
								  <code_scheme name="Project" version="174">
								    <JavaCodeStyleSettings>
								      <option name="CLASS_COUNT_TO_USE_IMPORT_ON_DEMAND" value="10" />
								      <option name="IMPORT_LAYOUT_TABLE">
								        <value>
								          <package name="" withSubpackages="true" static="false" />
								        </value>
								      </option>
								      <option name="NAMES_COUNT_TO_USE_IMPORT_ON_DEMAND" value="30" />
								    </JavaCodeStyleSettings>
								  </code_scheme>
								</component>"""
				);
	}

}
