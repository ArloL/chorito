package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.List;

import io.github.arlol.chorito.filter.FileIsGoneFilter;
import io.github.arlol.chorito.filter.FileIsGoneOrBinaryFilter;
import io.github.arlol.chorito.tools.ChoreContext.Builder;

public class PathChoreContext {

	public static Builder refresh(Builder builder) {
		Path root = builder.root();
		List<Path> textFiles = FilesSilent.walk(root)
				.filter(FilesSilent::isRegularFile)
				.filter(p -> !FileIsGoneOrBinaryFilter.fileIsGoneOrBinary(p))
				.toList();
		List<Path> files = FilesSilent.walk(root)
				.filter(FilesSilent::isRegularFile)
				.filter(p -> !FileIsGoneFilter.fileIsGone(p))
				.toList();
		return builder.textFiles(textFiles).files(files);
	}

	public static Builder newBuilder(Path root) {
		return refresh(new Builder(root, PathChoreContext::refresh, (r) -> {
		}));
	}

}
