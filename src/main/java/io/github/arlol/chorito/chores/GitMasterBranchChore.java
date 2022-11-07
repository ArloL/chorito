package io.github.arlol.chorito.chores;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.arlol.chorito.tools.ChoreContext;

public class GitMasterBranchChore {

	private ChoreContext context;

	public GitMasterBranchChore(ChoreContext context) {
		this.context = context;
	}

	@SuppressFBWarnings(
			value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE",
			justification = "FileRepositoryBuilder uses generics which spotbugs cant know"
	)
	public void doit() {
		try {
			context.root().toFile();
		} catch (UnsupportedOperationException e) {
			return;
		}
		FileRepositoryBuilder builder = new FileRepositoryBuilder()
				.setMustExist(false)
				.readEnvironment()
				.findGitDir(context.root().toFile());
		if (builder.getGitDir() == null && builder.getWorkTree() == null) {
			return;
		}
		try (Repository repository = builder.build()) {
			if (repository.getBranch().equalsIgnoreCase("master")) {
				throw new IllegalStateException("Rename master to main branch");
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
