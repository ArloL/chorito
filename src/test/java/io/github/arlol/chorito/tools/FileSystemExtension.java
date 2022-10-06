package io.github.arlol.chorito.tools;

import java.nio.file.FileSystem;
import java.nio.file.attribute.PosixFileAttributeView;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.github.marschall.memoryfilesystem.MemoryFileSystemBuilder;

public class FileSystemExtension
		implements BeforeEachCallback, AfterEachCallback {

	private FileSystem fileSystem;

	public FileSystem getFileSystem() {
		return this.fileSystem;
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		this.fileSystem = MemoryFileSystemBuilder.newEmpty()
				.addFileAttributeView(PosixFileAttributeView.class)
				.build("name");
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		if (this.fileSystem != null) {
			this.fileSystem.close();
		}
	}

}
