package io.github.arlol.chorito.tools;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.arlol.chorito.filter.FileIsGoneFilter;
import io.github.arlol.chorito.filter.FileIsGoneOrBinaryFilter;

public class GitChoreContext implements ChoreContext {

	private final Path root;
	private final List<Path> textFiles;
	private final List<Path> files;
	private final boolean hasGitHubRemote;

	public GitChoreContext(String root) {
		this(Paths.get(root).toAbsolutePath().normalize());
	}

	public GitChoreContext(Path root) {
		this(
				root,
				jgitResolveTextFiles(root),
				jgitResolveFiles(root),
				jgitHasGitHubRemote(root)
		);
	}

	public GitChoreContext(
			Path root,
			List<Path> textFiles,
			List<Path> files,
			boolean hasGitHubRemote
	) {
		this.root = root;
		this.textFiles = List.copyOf(textFiles);
		this.files = List.copyOf(files);
		this.hasGitHubRemote = hasGitHubRemote;
	}

	@Override
	public Path root() {
		return root;
	}

	@Override
	public List<Path> textFiles() {
		return List.copyOf(textFiles);
	}

	@Override
	public List<Path> files() {
		return List.copyOf(files);
	}

	@Override
	public Path resolve(Path path) {
		return root.resolve(path);
	}

	@Override
	public Path resolve(String path) {
		return root.resolve(path);
	}

	@Override
	public boolean hasGitHubRemote() {
		return hasGitHubRemote;
	}

	@Override
	public ChoreContext refresh() {
		return new GitChoreContext(
				this.root,
				textFiles.stream()
						.filter(p -> FilesSilent.exists(p))
						.collect(toList()),
				files.stream()
						.filter(p -> FilesSilent.exists(p))
						.collect(toList()),
				this.hasGitHubRemote
		);
	}

	@SuppressFBWarnings(
			value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
			justification = "FileRepositoryBuilder uses generics which spotbugs cant know"
	)
	private static List<Path> jgitResolveTextFiles(Path gitDir) {
		List<Path> result = new ArrayList<>();
		try (Repository repository = new FileRepositoryBuilder()
				.setMustExist(true)
				.readEnvironment()
				.findGitDir(gitDir.toFile())
				.build();
				RevWalk revWalk = new RevWalk(repository);
				TreeWalk treeWalk = new TreeWalk(repository)) {
			ObjectId headId = repository.resolve(Constants.HEAD);
			RevCommit headCommit = revWalk.parseCommit(headId);
			treeWalk.addTree(headCommit.getTree());
			while (treeWalk.next()) {
				if (treeWalk.isSubtree()) {
					treeWalk.enterSubtree();
				} else {
					Path path = Paths.get(treeWalk.getPathString());
					if (!FileIsGoneOrBinaryFilter.fileIsGoneOrBinary(path)) {
						result.add(path);
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return result;
	}

	@SuppressFBWarnings(
			value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
			justification = "FileRepositoryBuilder uses generics which spotbugs cant know"
	)
	private static List<Path> jgitResolveFiles(Path gitDir) {
		List<Path> result = new ArrayList<>();
		try (Repository repository = new FileRepositoryBuilder()
				.setMustExist(true)
				.readEnvironment()
				.findGitDir(gitDir.toFile())
				.build();
				RevWalk revWalk = new RevWalk(repository);
				TreeWalk treeWalk = new TreeWalk(repository)) {
			ObjectId headId = repository.resolve(Constants.HEAD);
			RevCommit headCommit = revWalk.parseCommit(headId);
			treeWalk.addTree(headCommit.getTree());
			while (treeWalk.next()) {
				if (treeWalk.isSubtree()) {
					treeWalk.enterSubtree();
				} else {
					Path path = Paths.get(treeWalk.getPathString());
					if (!FileIsGoneFilter.fileIsGone(path)) {
						result.add(path);
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return result;
	}

	@SuppressFBWarnings(
			value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
			justification = "FileRepositoryBuilder uses generics which spotbugs cant know"
	)
	private static boolean jgitHasGitHubRemote(Path gitDir) {
		try (Repository repository = new FileRepositoryBuilder()
				.setMustExist(true)
				.readEnvironment()
				.findGitDir(gitDir.toFile())
				.build();) {
			for (String remoteName : repository.getRemoteNames()) {
				Config config = repository.getConfig();
				String remoteUrl = config.getString(
						ConfigConstants.CONFIG_REMOTE_SECTION,
						remoteName,
						"url"
				);
				if (remoteUrl.startsWith("https://github.com")) {
					return true;
				}
			}
			return false;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
