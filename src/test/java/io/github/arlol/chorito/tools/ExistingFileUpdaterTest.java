package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ExistingFileUpdaterTest {

	private static final String SUFFIX = "# Add custom entries after this line to be preserved during automated updates";

	private static final String CONTENT = "managed content";

	private static final String DEFAULT = CONTENT + "\n" + SUFFIX + "\n";

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	private Path target() {
		return extension.root().resolve("file");
	}

	@Test
	public void testFileDoesNotExist() {
		ExistingFileUpdater.update(target(), CONTENT);
		assertThat(target()).content().isEqualTo(DEFAULT);
	}

	@Test
	public void testWithEmptyFile() {
		FilesSilent.writeString(target(), "");
		ExistingFileUpdater.update(target(), CONTENT);
		assertThat(target()).content().isEqualTo(DEFAULT);
	}

	@Test
	public void testWithNoMarker() {
		FilesSilent.writeString(target(), "custom content\n");
		ExistingFileUpdater.update(target(), CONTENT);
		assertThat(target()).content().isEqualTo(DEFAULT);
	}

	@Test
	public void testWithCurrentSuffix() {
		FilesSilent.write(
				target(),
				List.of(SUFFIX, "custom content"),
				"\n"
		);
		ExistingFileUpdater.update(target(), CONTENT);
		assertThat(target()).content()
				.isEqualTo(DEFAULT + "custom content\n");
	}

	@Test
	public void testStability() {
		ExistingFileUpdater.update(target(), CONTENT);
		ExistingFileUpdater.update(target(), CONTENT);
		assertThat(target()).content().isEqualTo(DEFAULT);
	}

	@Test
	public void testMigratesOldChoritoSuffix() {
		FilesSilent.write(
				target(),
				List.of(
						"# End of chorito. Add your ignores after this line and they will be preserved.",
						"custom content"
				),
				"\n"
		);
		ExistingFileUpdater.update(target(), CONTENT);
		assertThat(target()).content()
				.isEqualTo(DEFAULT + "custom content\n");
	}

	@Test
	public void testMigratesOldIgnoresSuffix() {
		FilesSilent.write(
				target(),
				List.of(
						"# Add custom ignores after this line to be preserved during automated updates",
						"custom content"
				),
				"\n"
		);
		ExistingFileUpdater.update(target(), CONTENT);
		assertThat(target()).content()
				.isEqualTo(DEFAULT + "custom content\n");
	}

	@Test
	public void testMigratesOldAttributesSuffix() {
		FilesSilent.write(
				target(),
				List.of(
						"# Add custom attributes after this line to be preserved during automated updates",
						"custom content"
				),
				"\n"
		);
		ExistingFileUpdater.update(target(), CONTENT);
		assertThat(target()).content()
				.isEqualTo(DEFAULT + "custom content\n");
	}

}
