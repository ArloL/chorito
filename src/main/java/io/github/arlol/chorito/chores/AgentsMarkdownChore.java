package io.github.arlol.chorito.chores;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

/**
 * Leaves every directory with a single regular {@code AGENTS.md} and no
 * {@code CLAUDE.md}.
 * <p>
 * Claude Code reads {@code AGENTS.md} itself now, so the symlink that used to
 * bridge the two names buys nothing and costs a checkout: Windows clones
 * without developer mode, and archive exports of any platform, turn a symlink
 * into a text file holding its target path, which whichever agent follows that
 * name then reads as instructions.
 */
public class AgentsMarkdownChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		Set<Path> directories = new LinkedHashSet<>();
		directories.add(context.root());
		context.files()
				.forEach(file -> directories.add(MyPaths.getParent(file)));
		directories.forEach(AgentsMarkdownChore::collapse);
		return context;
	}

	private static void collapse(Path directory) {
		Path agents = directory.resolve("AGENTS.md");
		Path claude = directory.resolve("CLAUDE.md");

		if (isRegularFileItself(agents)) {
			// The common case, and the one worth keeping cheap: AGENTS.md is
			// already what it should be, so CLAUDE.md goes without AGENTS.md
			// being rewritten. Rewriting it with its own bytes would leave
			// chorito touching a file it did not change in every repository
			// it ever runs in.
			FilesSilent.deleteIfExists(claude);
			return;
		}

		byte @Nullable [] content = surviving(agents, claude);
		// Read before either delete, because the two shapes this untangles
		// point at each other: an AGENTS.md symlink whose target is the
		// CLAUDE.md about to be deleted holds the only copy of the content.
		FilesSilent.deleteIfExists(claude);
		FilesSilent.deleteIfExists(agents);
		if (content != null) {
			FilesSilent.write(agents, content);
		}
	}

	/**
	 * The bytes AGENTS.md ends up with, or null when neither name leads to a
	 * readable file -- a dangling symlink left behind by whatever it once
	 * pointed at, which is deleted rather than resurrected.
	 * <p>
	 * A file outranks a symlink and AGENTS.md outranks CLAUDE.md, so two real
	 * files that drifted apart resolve to the AGENTS.md one.
	 */
	private static byte @Nullable [] surviving(Path agents, Path claude) {
		if (isRegularFileItself(claude)) {
			return FilesSilent.readAllBytes(claude);
		}
		if (FilesSilent.isRegularFile(agents)) {
			return FilesSilent.readAllBytes(agents);
		}
		if (FilesSilent.isRegularFile(claude)) {
			return FilesSilent.readAllBytes(claude);
		}
		return null;
	}

	/**
	 * Whether the path is a regular file and not a symlink to one.
	 * {@link Files#isRegularFile} alone cannot tell them apart, and the whole
	 * point of this chore is the difference.
	 */
	private static boolean isRegularFileItself(Path path) {
		return !Files.isSymbolicLink(path) && FilesSilent.isRegularFile(path);
	}

}
