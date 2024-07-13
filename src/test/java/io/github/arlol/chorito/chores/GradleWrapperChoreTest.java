package io.github.arlol.chorito.chores;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FakeProcessBuilderSilent;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class GradleWrapperChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new GradleWrapperChore().doit(extension.choreContext());
	}

	@Test
	public void test() throws Exception {
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(
						FakeProcessBuilderSilent
								.factory(this::fakeGradleWrapper)
				)
				.build();
		Path gradleProperties = context
				.resolve("gradle/wrapper/gradle-wrapper.properties");
		FilesSilent.writeString(
				gradleProperties,
				"""
						distributionBase=GRADLE_USER_HOME
						distributionPath=wrapper/dists
						distributionUrl=https\\://services.gradle.org/distributions/gradle-8.9-all.zip
						networkTimeout=10000
						validateDistributionUrl=true
						zipStoreBase=GRADLE_USER_HOME
						zipStorePath=wrapper/dists
						"""
		);
		new GradleWrapperChore().doit(context);
		assertTrue(FilesSilent.exists(gradleProperties));
	}

	private void fakeGradleWrapper() {
		var context = extension.choreContext();
		FilesSilent.touch(context.resolve("gradlew"));
		FilesSilent.touch(context.resolve("gradlew.bat"));
		FilesSilent.touch(
				context.resolve("gradle/wrapper/gradle-wrapper.properties")
		);
		FilesSilent.touch(context.resolve("gradle/wrapper/gradle-wrapper.jar"));
	}

}
