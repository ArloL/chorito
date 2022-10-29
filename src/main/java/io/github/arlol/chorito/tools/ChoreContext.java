package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.List;
import java.util.random.RandomGenerator;

public interface ChoreContext {

	public Path root();

	public List<Path> textFiles();

	public List<Path> files();

	public default Path resolve(Path path) {
		return root().resolve(path);
	}

	public default Path resolve(String path) {
		return root().resolve(path);
	}

	public boolean hasGitHubRemote();

	public ChoreContext refresh();

	public RandomGenerator randomGenerator();

}
