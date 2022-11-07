package io.github.arlol.chorito.tools;

import static java.util.Collections.emptyList;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.random.RandomGenerator;

public class ChoreContext {

	@FunctionalInterface
	public interface BuilderRefresh {

		Builder refresh(Builder builder);

	}

	public static class Builder {

		private Path root;
		private List<Path> textFiles = emptyList();
		private List<Path> files = emptyList();
		private boolean hasGitHubRemote = false;
		private RandomGenerator randomGenerator = new Random();
		private Clock clock = Clock.systemDefaultZone();
		private Function<String[], ProcessBuilderSilent> processBuilderFactory = ProcessBuilderSilent
				.factory();
		private final BuilderRefresh builderRefresh;

		public Builder(Path root, BuilderRefresh builderRefresh) {
			this.root = root;
			this.builderRefresh = builderRefresh;
		}

		public Builder(ChoreContext choreContext) {
			this.root = choreContext.root();
			this.textFiles = choreContext.textFiles();
			this.files = choreContext.files();
			this.hasGitHubRemote = choreContext.hasGitHubRemote();
			this.randomGenerator = choreContext.randomGenerator();
			this.clock = choreContext.clock();
			this.processBuilderFactory = choreContext.processBuilderFactory();
			this.builderRefresh = choreContext.builderRefresh;
		}

		public Path root() {
			return root;
		}

		public List<Path> textFiles() {
			return List.copyOf(textFiles);
		}

		public List<Path> files() {
			return List.copyOf(files);
		}

		public boolean hasGitHubRemote() {
			return hasGitHubRemote;
		}

		public RandomGenerator randomGenerator() {
			return randomGenerator;
		}

		public Clock clock() {
			return clock;
		}

		public Function<String[], ProcessBuilderSilent> processBuilderFactory() {
			return processBuilderFactory;
		}

		public Builder root(Path root) {
			this.root = root;
			return this;
		}

		public Builder textFiles(List<Path> textFiles) {
			this.textFiles = List.copyOf(textFiles);
			return this;
		}

		public Builder files(List<Path> files) {
			this.files = List.copyOf(files);
			return this;
		}

		public Builder hasGitHubRemote(boolean hasGitHubRemote) {
			this.hasGitHubRemote = hasGitHubRemote;
			return this;
		}

		public Builder randomGenerator(RandomGenerator randomGenerator) {
			this.randomGenerator = randomGenerator;
			return this;
		}

		public Builder clock(Clock clock) {
			this.clock = clock;
			return this;
		}

		public Builder processBuilderFactory(
				Function<String[], ProcessBuilderSilent> processBuilderFactory
		) {
			this.processBuilderFactory = processBuilderFactory;
			return this;
		}

		public ChoreContext build() {
			return new ChoreContext(
					root,
					textFiles,
					files,
					hasGitHubRemote,
					randomGenerator,
					clock,
					processBuilderFactory,
					builderRefresh
			);
		}

	}

	private final Path root;
	private final List<Path> textFiles;
	private final List<Path> files;
	private final boolean hasGitHubRemote;
	private final RandomGenerator randomGenerator;
	private final Clock clock;
	private final Function<String[], ProcessBuilderSilent> processBuilderFactory;
	private final BuilderRefresh builderRefresh;

	public ChoreContext(
			Path root,
			List<Path> textFiles,
			List<Path> files,
			boolean hasGitHubRemote,
			RandomGenerator randomGenerator,
			Clock clock,
			Function<String[], ProcessBuilderSilent> processBuilderFactory,
			BuilderRefresh builderRefresh
	) {
		this.root = root;
		this.textFiles = List.copyOf(textFiles);
		this.files = List.copyOf(files);
		this.hasGitHubRemote = hasGitHubRemote;
		this.randomGenerator = randomGenerator;
		this.clock = clock;
		this.processBuilderFactory = processBuilderFactory;
		this.builderRefresh = builderRefresh;
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public Path root() {
		return root;
	}

	public List<Path> textFiles() {
		return List.copyOf(textFiles);
	}

	public List<Path> files() {
		return List.copyOf(files);
	}

	public boolean hasGitHubRemote() {
		return hasGitHubRemote;
	}

	public RandomGenerator randomGenerator() {
		return randomGenerator;
	}

	public Path resolve(Path path) {
		return root().resolve(path);
	}

	public Path resolve(String path) {
		return root().resolve(path);
	}

	public Clock clock() {
		return clock;
	}

	public Function<String[], ProcessBuilderSilent> processBuilderFactory() {
		return processBuilderFactory;
	}

	public ProcessBuilderSilent newProcessBuilder(String... command) {
		return processBuilderFactory.apply(command);
	}

	public ChoreContext refresh() {
		return builderRefresh.refresh(toBuilder()).build();
	}

}
