package io.github.arlol.chorito.tools;

import static java.util.Collections.emptyList;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
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
		private List<String> remotes = emptyList();
		private RandomGenerator randomGenerator = new Random();
		private Clock clock = Clock.systemDefaultZone();
		private Function<String[], ProcessBuilderSilent> processBuilderFactory = ProcessBuilderSilent
				.factory();
		private final BuilderRefresh builderRefresh;
		private final Consumer<Path> deleteIgnoredFiles;

		public Builder(
				Path root,
				BuilderRefresh builderRefresh,
				Consumer<Path> deleteIgnoredFiles
		) {
			this.root = root;
			this.builderRefresh = builderRefresh;
			this.deleteIgnoredFiles = deleteIgnoredFiles;
		}

		public Builder(ChoreContext choreContext) {
			this.root = choreContext.root();
			this.textFiles = choreContext.textFiles();
			this.files = choreContext.files();
			this.remotes = choreContext.remotes();
			this.randomGenerator = choreContext.randomGenerator();
			this.clock = choreContext.clock();
			this.processBuilderFactory = choreContext.processBuilderFactory();
			this.builderRefresh = choreContext.builderRefresh;
			this.deleteIgnoredFiles = choreContext.deleteIgnoredFiles;
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

		public List<String> remotes() {
			return List.copyOf(remotes);
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

		public Builder remotes(List<String> remotes) {
			this.remotes = List.copyOf(remotes);
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
					remotes,
					randomGenerator,
					clock,
					processBuilderFactory,
					builderRefresh,
					deleteIgnoredFiles
			);
		}

	}

	private final Path root;
	private final List<Path> textFiles;
	private final List<Path> files;
	private final List<String> remotes;
	private final RandomGenerator randomGenerator;
	private final Clock clock;
	private final Function<String[], ProcessBuilderSilent> processBuilderFactory;
	private final BuilderRefresh builderRefresh;
	private final Consumer<Path> deleteIgnoredFiles;
	private boolean dirty;

	public ChoreContext(
			Path root,
			List<Path> textFiles,
			List<Path> files,
			List<String> remotes,
			RandomGenerator randomGenerator,
			Clock clock,
			Function<String[], ProcessBuilderSilent> processBuilderFactory,
			BuilderRefresh builderRefresh,
			Consumer<Path> deleteIgnoredFilesConsumer
	) {
		this.root = root;
		this.textFiles = List.copyOf(textFiles);
		this.files = List.copyOf(files);
		this.remotes = List.copyOf(remotes);
		this.randomGenerator = randomGenerator;
		this.clock = clock;
		this.processBuilderFactory = processBuilderFactory;
		this.builderRefresh = builderRefresh;
		this.deleteIgnoredFiles = deleteIgnoredFilesConsumer;
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

	public List<String> remotes() {
		return List.copyOf(remotes);
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

	public void setDirty() {
		setDirty(true);
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public boolean isDirty() {
		return this.dirty;
	}

	public void deleteIgnoredFiles() {
		deleteIgnoredFiles.accept(root);
	}

}
