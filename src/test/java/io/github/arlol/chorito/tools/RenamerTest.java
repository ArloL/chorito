package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class RenamerTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	void testName() throws Exception {
		Path before = extension.choreContext().resolve("hey-replaceme-jude");
		FilesSilent.writeString(before, "");

		Renamer.replaceInFilename(before, "-replaceme", "");

		assertThat(FilesSilent.exists(before)).isFalse();
		Path expected = extension.choreContext().resolve("hey-jude");
		assertThat(FilesSilent.exists(expected)).isTrue();
	}

}
