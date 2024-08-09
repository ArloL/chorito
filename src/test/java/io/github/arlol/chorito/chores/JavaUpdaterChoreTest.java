package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class JavaUpdaterChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		doit();
	}

	private void doit() {
		new JavaUpdaterChore().doit(extension.choreContext());
	}

	@Test
	public void testEclipseSettings() throws Exception {
		Path prefs = extension.root()
				.resolve(".settings/org.eclipse.jdt.core.prefs");
		FilesSilent.writeString(prefs, """
				org.eclipse.jdt.core.compiler.source=11
				org.eclipse.jdt.core.compiler.compliance=11
				org.eclipse.jdt.core.compiler.codegen.targetPlatform=11
				""");

		doit();

		assertThat(prefs).content().isEqualTo("""
				org.eclipse.jdt.core.compiler.codegen.targetPlatform=21
				org.eclipse.jdt.core.compiler.compliance=21
				org.eclipse.jdt.core.compiler.source=21
						""");
	}

	@Test
	public void testPomXml() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pom, "<java.version>11</java.version>");

		doit();

		assertThat(pom).content().isEqualTo("<java.version>21</java.version>");
	}

	@Test
	public void testWorkflow() throws Exception {
		Path workflow = extension.root().resolve(".github/workflows/main.yaml");
		FilesSilent.writeString(workflow, "  JAVA_VERSION: 11");

		doit();

		assertThat(workflow).content().isEqualTo("  JAVA_VERSION: 21\n");
	}

	@Test
	public void testJitpackJdk8() throws Exception {
		Path jitpack = extension.root().resolve("jitpack.yml");
		FilesSilent.writeString(jitpack, "- openjdk8");

		doit();

		assertThat(jitpack).content().isEqualTo("- openjdk21\n");
	}

	@Test
	public void testJitpackJdk11() throws Exception {
		Path jitpack = extension.root().resolve("jitpack.yml");
		FilesSilent.writeString(jitpack, "- openjdk11");

		doit();

		assertThat(jitpack).content().isEqualTo("- openjdk21\n");
	}

	@Test
	public void testPomWithoutJavaVersionXml() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pom, """
				<project>
					<properties>
						<revision>0.0.1-SNAPSHOT</revision>
					</properties>
				</project>
				""");

		doit();

		assertThat(pom).content().isEqualTo("""
				<project>
					<properties>
						<revision>0.0.1-SNAPSHOT</revision>
						<java.version>21</java.version>
					</properties>
				</project>
				""");
	}

	@Test
	public void testPomWithoutProperties() throws Exception {
		Path pom = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pom, """
				<project>
				</project>
				""");

		doit();

		assertThat(pom).content().isEqualTo("""
				<project>
					<properties>
						<java.version>21</java.version>
					</properties>
				</project>
				""");
	}

}
