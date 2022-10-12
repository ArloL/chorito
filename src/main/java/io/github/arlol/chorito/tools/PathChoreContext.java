package io.github.arlol.chorito.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.github.arlol.chorito.filter.FileIsGoneFilter;
import io.github.arlol.chorito.filter.FileIsGoneOrBinaryFilter;

public class PathChoreContext implements ChoreContext {

	private final Path root;
	private final List<Path> textFiles;
	private final List<Path> files;

	public PathChoreContext(String root) {
		this(Paths.get(root).toAbsolutePath().normalize());
	}

	public PathChoreContext(Path root) {
		this(root, resolveTextFiles(root), resolveFiles(root));
	}

	public PathChoreContext(Path root, List<Path> textFiles, List<Path> files) {
		this.root = root;
		this.textFiles = List.copyOf(textFiles);
		this.files = List.copyOf(files);
	}

	@Override
	public Path root() {
		return root;
	}

	@Override
	public List<Path> textFiles() {
		return textFiles;
	}

	@Override
	public List<Path> files() {
		return files;
	}

	@Override
	public boolean hasGitHubRemote() {
		return false;
	}

	private static List<Path> resolveFiles(Path path) {
		return FilesSilent.walk(path)
				.filter(Files::isRegularFile)
				.filter(p -> !FileIsGoneFilter.fileIsGone(p))
				.toList();
	}

	private static List<Path> resolveTextFiles(Path path) {
		return FilesSilent.walk(path)
				.filter(Files::isRegularFile)
				.filter(p -> !FileIsGoneOrBinaryFilter.fileIsGoneOrBinary(p))
				.toList();
	}

	@Override
	public ChoreContext refresh() {
		return new PathChoreContext(root);
	}

}
