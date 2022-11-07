package org.ec4j.lint.api;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ec4j.lint.api.LinterRegistry.Builder;
import org.ec4j.lint.api.LinterRegistry.LinterEntry;

public class CustomLinterRegistryBuilder extends LinterRegistry.Builder {

	public static class CustomPathSet extends PathSet {

		private final List<PathMatcher> includes;
		private final List<PathMatcher> excludes;
		private final Path currentDir;

		CustomPathSet(
				List<PathMatcher> includes,
				List<PathMatcher> excludes,
				Path currentDir
		) {
			super(includes, excludes);
			this.includes = includes;
			this.excludes = excludes;
			this.currentDir = currentDir;
		}

		@Override
		public boolean contains(Path path) {
			path = currentDir.resolve(path);
			for (PathMatcher exclude : excludes) {
				if (exclude.matches(path)) {
					return false;
				}
			}
			for (PathMatcher include : includes) {
				if (include.matches(path)) {
					return true;
				}
			}
			return false;
		}

	}

	public static class CustomLinterEntryBuilder extends LinterEntry.Builder {

		private final Linter linter;
		private final PathSet.Builder pathSetBuilder;
		private boolean useDefaultIncludesAndExcludes = true;

		CustomLinterEntryBuilder(Linter linter, Path currentDir) {
			super(linter);
			this.linter = linter;
			this.pathSetBuilder = new CustomPathSetBuilder(currentDir);
		}

		/**
		 * @return a new {@link LinterEntry}
		 */
		@Override
		public LinterEntry build() {
			if (this.useDefaultIncludesAndExcludes) {
				pathSetBuilder.includes(linter.getDefaultIncludes());
				pathSetBuilder.excludes(linter.getDefaultExcludes());
			}
			return new LinterEntry(linter, pathSetBuilder.build());
		}

	}

	public static class CustomPathSetBuilder extends PathSet.Builder {

		private List<PathMatcher> excludes = new ArrayList<>();
		private List<PathMatcher> includes = new ArrayList<>();
		private final Path currentDir;
		private final FileSystem fileSystem;

		CustomPathSetBuilder(Path currentDir) {
			super();
			this.currentDir = currentDir;
			this.fileSystem = currentDir.getFileSystem();
		}

		/**
		 * @return a new {@link PathSet}
		 */
		@Override
		public CustomPathSet build() {
			List<PathMatcher> useExcludes = this.excludes;
			this.excludes = null;
			List<PathMatcher> useIncludes = this.includes;
			this.includes = null;
			return new CustomPathSet(
					Collections.unmodifiableList(useIncludes),
					Collections.unmodifiableList(useExcludes),
					this.currentDir
			);
		}

		private PathMatcher getPathMatcher(String glob) {
			if (glob.equalsIgnoreCase("**/*")) {
				return fileSystem.getPathMatcher("regex:.*");
			} else {
				return fileSystem.getPathMatcher("glob:" + glob);
			}
		}

		/**
		 * Adds an exclude glob
		 *
		 * @param glob the glob to add
		 * @return this {@link Builder}
		 */
		@Override
		public CustomPathSetBuilder exclude(String glob) {
			excludes.add(getPathMatcher(glob));
			return this;
		}

		/**
		 * Adds multiple exclude globs
		 *
		 * @param globs the globs to add
		 * @return this {@link Builder}
		 */
		@Override
		public CustomPathSetBuilder excludes(List<String> globs) {
			if (globs != null) {
				for (String glob : globs) {
					exclude(glob);
				}
			}
			return this;
		}

		/**
		 * Adds multiple exclude globs
		 *
		 * @param globs the globs to add
		 * @return this {@link Builder}
		 */
		@Override
		public CustomPathSetBuilder excludes(String... globs) {
			if (globs != null) {
				for (String glob : globs) {
					exclude(glob);
				}
			}
			return this;
		}

		/**
		 * Adds an include glob
		 *
		 * @param glob the glob to add
		 * @return this {@link Builder}
		 */
		@Override
		public CustomPathSetBuilder include(String glob) {
			includes.add(getPathMatcher(glob));
			return this;
		}

		/**
		 * Adds multiple include globs
		 *
		 * @param globs the globs to add
		 * @return this {@link Builder}
		 */
		@Override
		public CustomPathSetBuilder includes(List<String> globs) {
			for (String glob : globs) {
				include(glob);
			}
			return this;
		}

		/**
		 * Adds multiple include globs
		 *
		 * @param globs the globs to add
		 * @return this {@link Builder}
		 */
		@Override
		public CustomPathSetBuilder includes(String... globs) {
			if (globs != null) {
				for (String glob : globs) {
					include(glob);
				}
			}
			return this;
		}

	}

	private final Map<String, CustomLinterEntryBuilder> entries = new LinkedHashMap<>();
	private Logger log;
	private Path currentDir;

	public CustomLinterRegistryBuilder(Path currentDir) {
		this.currentDir = currentDir;
	}

	@Override
	public LinterRegistry build() {
		Map<String, LinterEntry> useEntries = new LinkedHashMap<>(
				entries.size()
		);
		for (Map.Entry<String, CustomLinterEntryBuilder> en : entries
				.entrySet()) {
			useEntries.put(en.getKey(), en.getValue().build());
		}
		return new LinterRegistry(Collections.unmodifiableMap(useEntries), log);
	}

	@Override
	public Builder entry(Linter linter) {
		final String linterClass = linter.getClass().getName();
		CustomLinterEntryBuilder en = entries.get(linterClass);
		if (en == null) {
			en = new CustomLinterEntryBuilder(linter, currentDir);

			entries.put(linterClass, en);
		}

		return this;
	}

	@Override
	public Builder log(Logger log) {
		this.log = log;
		return this;
	}

	@Override
	public Builder removeEntry(String id) {
		entries.remove(id);
		return this;
	}

}
