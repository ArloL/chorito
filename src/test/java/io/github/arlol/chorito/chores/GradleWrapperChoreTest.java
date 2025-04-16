package io.github.arlol.chorito.chores;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FakeProcessBuilderSilent;
import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.ProcessBuilderSilent;

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
		FilesSilent.touch(context.resolve("gradlew"));
		Path gradleProperties = context
				.resolve("gradle/wrapper/gradle-wrapper.properties");
		FilesSilent.writeString(
				gradleProperties,
				"""
						distributionBase=GRADLE_USER_HOME
						distributionPath=wrapper/dists
						distributionUrl=https\\://services.gradle.org/distributions/gradle-8.13-all.zip
						networkTimeout=10000
						validateDistributionUrl=true
						zipStoreBase=GRADLE_USER_HOME
						zipStorePath=wrapper/dists
						"""
		);
		new GradleWrapperChore().doit(context);
		assertTrue(FilesSilent.exists(gradleProperties));
	}

	@Test
	public void testNestedGradlew() throws Exception {
		var context = extension.choreContext()
				.toBuilder()
				.processBuilderFactory(
						FakeProcessBuilderSilent
								.factory(this::fakeGradleWrapper)
				)
				.build();
		FilesSilent.touch(context.resolve("nested/settings.gradle"));
		Path gradleProperties = context
				.resolve("nested/gradle/wrapper/gradle-wrapper.properties");
		FilesSilent.writeString(
				gradleProperties,
				"""
						distributionBase=GRADLE_USER_HOME
						distributionPath=wrapper/dists
						distributionUrl=https\\://services.gradle.org/distributions/gradle-8.13-bin.zip
						networkTimeout=10000
						validateDistributionUrl=true
						zipStoreBase=GRADLE_USER_HOME
						zipStorePath=wrapper/dists
						"""
		);

		// when
		new GradleWrapperChore().doit(context.refresh());

		// the
		assertTrue(FilesSilent.exists(gradleProperties));
		assertTrue(FilesSilent.exists(context.resolve("nested/gradlew.bat")));
	}

	private void fakeGradleWrapper(ProcessBuilderSilent processBuilderSilent) {
		Path directory = processBuilderSilent.directory();
		FilesSilent.touch(directory.resolve("gradlew"));
		FilesSilent.touch(directory.resolve("gradlew.bat"));
		FilesSilent.touch(
				directory.resolve("gradle/wrapper/gradle-wrapper.properties")
		);
		FilesSilent
				.touch(directory.resolve("gradle/wrapper/gradle-wrapper.jar"));
	}

}
