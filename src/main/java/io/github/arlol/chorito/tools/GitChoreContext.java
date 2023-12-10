package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.arlol.chorito.filter.FileIsGoneFilter;
import io.github.arlol.chorito.filter.FileIsGoneOrBinaryFilter;
import io.github.arlol.chorito.tools.ChoreContext.Builder;

public class GitChoreContext {

	@SuppressFBWarnings(
			value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
			justification = "FileRepositoryBuilder uses generics which spotbugs cant know"
	)
	public static Builder refresh(Builder builder) {
		Path root = builder.root();
		List<Path> textFiles = new ArrayList<>();
		List<Path> files = new ArrayList<>();
		List<String> remotes = new ArrayList<>();
		boolean hasGitHubRemote = false;

		try (Repository repository = new FileRepositoryBuilder()
				.setMustExist(true)
				.readEnvironment()
				.findGitDir(root.toFile())
				.build()) {

			for (String remoteName : repository.getRemoteNames()) {
				Config config = repository.getConfig();
				String remoteUrl = config.getString(
						ConfigConstants.CONFIG_REMOTE_SECTION,
						remoteName,
						"url"
				);
				remotes.add(remoteUrl);
				if (remoteUrl.startsWith("https://github.com")) {
					hasGitHubRemote = true;
				}
			}

			DirCache dirCache = new AddCommand(repository).addFilepattern(".")
					.call();
			for (DirCacheEntry dirCacheEntry : dirCache.getEntriesWithin("")) {
				Path path = root.resolve(dirCacheEntry.getPathString());
				if (!FileIsGoneFilter.fileIsGone(path)) {
					files.add(path);
				}
				if (!FileIsGoneOrBinaryFilter.fileIsGoneOrBinary(path)) {
					textFiles.add(path);
				}
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (GitAPIException e) {
			throw new IllegalStateException(e);
		}

		return builder.textFiles(textFiles)
				.files(files)
				.remotes(remotes)
				.hasGitHubRemote(hasGitHubRemote);
	}

	@SuppressFBWarnings(
			value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
			justification = "FileRepositoryBuilder uses generics which spotbugs cant know"
	)
	public static void deleteIgnoredFiles(Path root) {
		try (Repository repository = new FileRepositoryBuilder()
				.setMustExist(true)
				.readEnvironment()
				.findGitDir(root.toFile())
				.build(); TreeWalk treeWalk = new TreeWalk(repository);) {
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
				DirCacheIterator c = treeWalk
						.getTree(0, DirCacheIterator.class);
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
		Path root = Paths.get(rootString).toAbsolutePath().normalize();
		return refresh(
				new Builder(
						root,
						GitChoreContext::refresh,
						GitChoreContext::deleteIgnoredFiles
				)
		);
	}

}
