package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.jspecify.annotations.Nullable;

import io.github.arlol.chorito.filter.FileIsGoneFilter;
import io.github.arlol.chorito.filter.FileIsGoneOrBinaryFilter;
import io.github.arlol.chorito.tools.ChoreContext.Builder;

public class GitChoreContext {

	public static Builder refresh(Builder builder) {
		Path root = builder.root();
		List<Path> textFiles = new ArrayList<>();
		List<Path> files = new ArrayList<>();
		List<String> remotes = new ArrayList<>();

		var fileRepositoryBuilder = new FileRepositoryBuilder()
				.setMustExist(true)
				.readEnvironment()
				.findGitDir(root.toFile());
		try (Repository repository = fileRepositoryBuilder.build();
				TreeWalk treeWalk = new TreeWalk(repository);) {

			for (String remoteName : repository.getRemoteNames()) {
				Config config = repository.getConfig();
				String remoteUrl = config.getString(
						ConfigConstants.CONFIG_REMOTE_SECTION,
						remoteName,
						"url"
				);
				remotes.add(remoteUrl);
			}

			treeWalk.addTree(
					new DirCacheBuildIterator(
							repository.readDirCache().builder()
					)
			);

			WorkingTreeIterator workingTreeIterator = new FileTreeIterator(
					repository
			);
			workingTreeIterator.setDirCacheIterator(treeWalk, 0);
			treeWalk.setRecursive(true);
			treeWalk.addTree(workingTreeIterator);
			while (treeWalk.next()) {
				@Nullable
				DirCacheIterator c = treeWalk
						.getTree(0, DirCacheIterator.class);
				@Nullable
				WorkingTreeIterator file = treeWalk
						.getTree(1, WorkingTreeIterator.class);
				if (c != null && file != null && file.isEntryIgnored()) {
					continue;
				}
				Path path = root.resolve(treeWalk.getPathString());
				if (Files.isSymbolicLink(path)) {
					continue;
				}
				if (!FileIsGoneFilter.fileIsGone(path)) {
					files.add(path);
				}
				if (!FileIsGoneOrBinaryFilter.fileIsGoneOrBinary(path)) {
					textFiles.add(path);
				}
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return builder.textFiles(textFiles).files(files).remotes(remotes);
	}

	public static void deleteIgnoredFiles(Path root) {
		var fileRepositoryBuilder = new FileRepositoryBuilder()
				.setMustExist(true)
				.readEnvironment()
				.findGitDir(root.toFile());
		try (Repository repository = fileRepositoryBuilder.build();
				TreeWalk treeWalk = new TreeWalk(repository);) {
			treeWalk.addTree(
					new DirCacheBuildIterator(
							repository.readDirCache().builder()
					)
			);

			WorkingTreeIterator workingTreeIterator = new FileTreeIterator(
					repository
			);
			workingTreeIterator.setDirCacheIterator(treeWalk, 0);
			treeWalk.setRecursive(true);
			treeWalk.addTree(workingTreeIterator);
			while (treeWalk.next()) {
				@Nullable
				DirCacheIterator c = treeWalk
						.getTree(0, DirCacheIterator.class);
				@Nullable
				WorkingTreeIterator f = treeWalk
						.getTree(1, WorkingTreeIterator.class);
				if (c != null && f != null && f.isEntryIgnored()) {
					Path path = root.resolve(treeWalk.getPathString());
					FilesSilent.deleteIfExists(path);
					continue;
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static Builder newBuilder(String rootString) {
		Path root = Path.of(rootString).toAbsolutePath().normalize();
		return refresh(
				new Builder(
						root,
						GitChoreContext::refresh,
						GitChoreContext::deleteIgnoredFiles
				)
		);
	}

}
