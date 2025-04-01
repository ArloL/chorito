package io.github.arlol.chorito.chores;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import io.github.arlol.chorito.tools.ChoreContext;

public class GitMasterBranchChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		try {
			context.root().toFile();
		} catch (UnsupportedOperationException e) {
			return context;
		}
		var builder = new FileRepositoryBuilder().setMustExist(false)
				.readEnvironment()
				.findGitDir(context.root().toFile());
		if (builder.getGitDir() == null && builder.getWorkTree() == null) {
			return context;
		}
		try (Repository repository = builder.build()) {
			if (repository.getBranch().equalsIgnoreCase("master")) {
				System.out.println("You should rename master to main branch");
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return context;
	}

}
