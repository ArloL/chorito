package io.github.arlol.chorito.tools;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

public class FileSystemExtension
		implements BeforeEachCallback, AfterEachCallback {

	private FileSystem fileSystem;

	public FileSystem fileSystem() {
		return this.fileSystem;
	}

	public Path root() {
		Path root = this.fileSystem.getPath("/app");
		FilesSilent.createDirectories(root);
		return root;
	}

	public ChoreContext choreContext() {
		return PathChoreContext.newBuilder(root()).build();
	}

	/**
	 * Every file and directory below {@link #root()}, relative to it and
	 * sorted, so that a test can pin down exactly what a chore created.
	 */
	public List<String> relativePaths() {
		Path root = root();
		try (Stream<Path> walk = FilesSilent.walk(root)) {
			return walk.filter(path -> !path.equals(root))
					.map(path -> root.relativize(path).toString())
					.sorted()
					.toList();
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		this.fileSystem = MemoryFileSystemBuilder.newEmpty()
				.addFileAttributeView(PosixFileAttributeView.class)
				.build();
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		if (this.fileSystem != null) {
			this.fileSystem.close();
		}
	}

}
