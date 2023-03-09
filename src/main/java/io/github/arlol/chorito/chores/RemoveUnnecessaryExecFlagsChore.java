package io.github.arlol.chorito.chores;

import java.io.UncheckedIOException;
import java.nio.charset.MalformedInputException;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.ExecutableFlagger;

public class RemoveUnnecessaryExecFlagsChore {

	private final ChoreContext context;

	public RemoveUnnecessaryExecFlagsChore(ChoreContext context) {
		this.context = context.refresh();
	}

	public void doit() {
		context.textFiles().forEach(path -> {
			if (ExecutableFlagger.isExecutable(path)) {
				try {
					String readString = FilesSilent.readString(path);
					if (!readString.startsWith("#!")) {
						ExecutableFlagger.makeNotExecutableIfPossible(path);
					}
				} catch (UncheckedIOException e) {
					if (!(e.getCause() instanceof MalformedInputException)) {
						throw e;
					}
				}
			}
		});
	}

}
