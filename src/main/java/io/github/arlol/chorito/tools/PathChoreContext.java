package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.random.RandomGenerator;

import io.github.arlol.chorito.filter.FileIsGoneFilter;
import io.github.arlol.chorito.filter.FileIsGoneOrBinaryFilter;

public class PathChoreContext implements ChoreContext {

	public static class Builder {

		private final Path root;
		private final boolean hasGitHubRemote;
		private final RandomGenerator randomGenerator;
		private final Clock clock;
		private final Function<String[], SilentProcessBuilder> processBuilderFactory;

		public Builder() {
			this(
					null,
					false,
					new Random(),
					Clock.systemDefaultZone(),
					(command) -> SilentProcessBuilder.create(command)
			);
		}

		public Builder(
				Path root,
				boolean hasGitHubRemote,
				RandomGenerator randomGenerator,
				Clock clock,
				Function<String[], SilentProcessBuilder> processBuilderFactory
		) {
			super();
			this.root = root;
			this.hasGitHubRemote = hasGitHubRemote;
			this.randomGenerator = randomGenerator;
			this.clock = clock;
			this.processBuilderFactory = processBuilderFactory;
		}

		public Builder root(Path root) {
			return new Builder(
					root,
					hasGitHubRemote,
					randomGenerator,
					clock,
					processBuilderFactory
			);
		}

		public Builder hasGitHubRemote(boolean hasGitHubRemote) {
			return new Builder(
					root,
					hasGitHubRemote,
					randomGenerator,
					clock,
					processBuilderFactory
			);
		}

		public Builder randomGenerator(RandomGenerator randomGenerator) {
			return new Builder(
					root,
					hasGitHubRemote,
					randomGenerator,
					clock,
					processBuilderFactory
			);
		}

		public Builder clock(Clock clock) {
			return new Builder(
					root,
					hasGitHubRemote,
					randomGenerator,
					clock,
					processBuilderFactory
			);
		}

		public Builder processBuilderFactory(
				Function<String[], SilentProcessBuilder> processBuilderFactory
		) {
			return new Builder(
					root,
					hasGitHubRemote,
					randomGenerator,
					clock,
					processBuilderFactory
			);
		}

		public PathChoreContext build() {
			return new PathChoreContext(
					root,
					hasGitHubRemote,
					randomGenerator,
					clock,
					processBuilderFactory
			);
		}

	}

	public static Builder newBuilder() {
		return new Builder();
	}

	private final Path root;
	private final List<Path> textFiles;
	private final List<Path> files;
	private final boolean hasGitHubRemote;
	private final RandomGenerator randomGenerator;
	private final Clock clock;
	private final Function<String[], SilentProcessBuilder> processBuilderFactory;

	public PathChoreContext(
			Path root,
			boolean hasGitHubRemote,
			RandomGenerator randomGenerator,
			Clock clock,
			Function<String[], SilentProcessBuilder> processBuilderFactory
	) {
		this.root = root;
		this.textFiles = resolveTextFiles(root);
		this.files = resolveFiles(root);
		this.hasGitHubRemote = hasGitHubRemote;
		this.randomGenerator = randomGenerator;
		this.clock = clock;
		this.processBuilderFactory = processBuilderFactory;
	}

	public Builder toBuilder() {
		return new Builder().root(root)
				.hasGitHubRemote(hasGitHubRemote)
				.randomGenerator(randomGenerator)
				.clock(clock)
				.randomGenerator(randomGenerator)
				.clock(clock)
				.processBuilderFactory(processBuilderFactory);
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
		return hasGitHubRemote;
	}

	private static List<Path> resolveFiles(Path path) {
		return FilesSilent.walk(path)
				.filter(FilesSilent::isRegularFile)
				.filter(p -> !FileIsGoneFilter.fileIsGone(p))
				.toList();
	}

	private static List<Path> resolveTextFiles(Path path) {
		return FilesSilent.walk(path)
				.filter(FilesSilent::isRegularFile)
				.filter(p -> !FileIsGoneOrBinaryFilter.fileIsGoneOrBinary(p))
				.toList();
	}

	@Override
	public ChoreContext refresh() {
		return toBuilder().build();
	}

	@Override
	public RandomGenerator randomGenerator() {
		return randomGenerator;
	}

	@Override
	public Clock clock() {
		return clock;
	}

	@Override
	public SilentProcessBuilder newProcessBuilder(String... command) {
		return processBuilderFactory.apply(command);
	}

}
