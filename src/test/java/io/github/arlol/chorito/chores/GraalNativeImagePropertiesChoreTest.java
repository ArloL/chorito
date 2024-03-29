package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GraalNativeImagePropertiesChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new GraalNativeImagePropertiesChore().doit(extension.choreContext());
	}

	@Test
	public void test() throws Exception {
		ChoreContext context = context();

		Path nativeImageProperties = context.resolve(
				"src/main/resources/META-INF/native-image/io.github.arlol/chorito/native-image.properties"
		);
		FilesSilent.writeString(
				nativeImageProperties,
				"Args = \\\n" + "-H:+ReportExceptionStackTraces \\\n"
						+ "--no-fallback \\\n"
						+ "--allow-incomplete-classpath \\\n"
						+ "--initialize-at-build-time=\\\n"
						+ "org.eclipse.jgit.lib.ObjectId,\\\n"
						+ "org.eclipse.jgit.diff.RenameDetector,\\\n"
						+ "org.eclipse.jgit.diff.DiffEntry,\\\n"
						+ "org.eclipse.jgit.ignore.internal.Strings,\\\n"
						+ "org.eclipse.jgit.attributes.AttributesHandler,\\\n"
						+ "org.eclipse.jgit.diff.RenameDetector,\\\n"
						+ "org.eclipse.jgit.diff.DiffEntry,\\\n"
						+ "org.eclipse.jgit.ignore.internal.Strings,\\\n"
						+ "org.eclipse.jgit.attributes.AttributesHandler\n" + ""
		);

		String before = FilesSilent.readString(nativeImageProperties);
		assertThat(before).contains("--allow-incomplete-classpath");
		new GraalNativeImagePropertiesChore().doit(context);

		String actual = FilesSilent.readString(nativeImageProperties);
		assertThat(actual).doesNotContain("--allow-incomplete-classpath");
		assertEquals(
				"Args = \\\n" + "-H:+ReportExceptionStackTraces \\\n"
						+ "--no-fallback \\\n"
						+ "--initialize-at-build-time=\\\n"
						+ "org.eclipse.jgit.lib.ObjectId,\\\n"
						+ "org.eclipse.jgit.diff.RenameDetector,\\\n"
						+ "org.eclipse.jgit.diff.DiffEntry,\\\n"
						+ "org.eclipse.jgit.ignore.internal.Strings,\\\n"
						+ "org.eclipse.jgit.attributes.AttributesHandler,\\\n"
						+ "org.eclipse.jgit.diff.RenameDetector,\\\n"
						+ "org.eclipse.jgit.diff.DiffEntry,\\\n"
						+ "org.eclipse.jgit.ignore.internal.Strings,\\\n"
						+ "org.eclipse.jgit.attributes.AttributesHandler\n"
						+ "",
				actual
		);
	}

	private ChoreContext context() {
		ChoreContext context = extension.choreContext();
		FilesSilent.writeString(
				context.resolve(
						"src/main/resources/META-INF/native-image/io.github.arlol/chorito/native-image.properties"
				),
				"Args = \\\n" + "-H:+ReportExceptionStackTraces \\\n"
						+ "--no-fallback \\\n"
						+ "--allow-incomplete-classpath \\\n"
						+ "--initialize-at-build-time=\\\n"
						+ "org.eclipse.jgit.lib.ObjectId,\\\n"
						+ "org.eclipse.jgit.diff.RenameDetector,\\\n"
						+ "org.eclipse.jgit.diff.DiffEntry,\\\n"
						+ "org.eclipse.jgit.ignore.internal.Strings,\\\n"
						+ "org.eclipse.jgit.attributes.AttributesHandler,\\\n"
						+ "org.eclipse.jgit.diff.RenameDetector,\\\n"
						+ "org.eclipse.jgit.diff.DiffEntry,\\\n"
						+ "org.eclipse.jgit.ignore.internal.Strings,\\\n"
						+ "org.eclipse.jgit.attributes.AttributesHandler\n" + ""
		);
		return context;
	}

}
