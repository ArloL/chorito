package io.github.arlol.chorito.chores;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GradleWrapperChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new GradleWrapperChore(extension.choreContext()).doit();
	}

	@Test
	public void test() throws Exception {
		ChoreContext context = extension.choreContext();
		Path gradleProperties = context
				.resolve("gradle/wrapper/gradle-wrapper.properties");
		FilesSilent.writeString(
				gradleProperties,
				"""
						distributionBase=GRADLE_USER_HOME
						distributionPath=wrapper/dists
						distributionUrl=https\\://services.gradle.org/distributions/gradle-8.0.2-all.zip
						networkTimeout=10000
						zipStoreBase=GRADLE_USER_HOME
						zipStorePath=wrapper/dists
						"""
		);
		new GradleWrapperChore(context).doit();
		assertTrue(FilesSilent.exists(gradleProperties));
	}

}
