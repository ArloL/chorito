package io.github.arlol.chorito.tools;

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
				.build();
				RevWalk revWalk = new RevWalk(repository);
				TreeWalk treeWalk = new TreeWalk(repository)) {

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

			ObjectId headId = repository.resolve(Constants.HEAD);
			RevCommit headCommit = revWalk.parseCommit(headId);
			treeWalk.addTree(headCommit.getTree());
			while (treeWalk.next()) {
				if (treeWalk.isSubtree()) {
					treeWalk.enterSubtree();
				} else {
					Path path = root.resolve(treeWalk.getPathString());
					if (!FileIsGoneFilter.fileIsGone(path)) {
						files.add(path);
					}
					if (!FileIsGoneOrBinaryFilter.fileIsGoneOrBinary(path)) {
						textFiles.add(path);
					}
				}
			}

		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return builder.textFiles(textFiles)
				.files(files)
				.remotes(remotes)
				.hasGitHubRemote(hasGitHubRemote);
	}

	public static Builder newBuilder(String rootString) {
		Path root = Paths.get(rootString).toAbsolutePath().normalize();
		return refresh(new Builder(root, GitChoreContext::refresh));
	}

}
