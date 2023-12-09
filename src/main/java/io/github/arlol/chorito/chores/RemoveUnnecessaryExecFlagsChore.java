package io.github.arlol.chorito.chores;

import java.io.UncheckedIOException;
import java.nio.charset.MalformedInputException;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ExecutableFlagger;
import io.github.arlol.chorito.tools.FilesSilent;

public class RemoveUnnecessaryExecFlagsChore implements Chore {

	@Override
	public void doit(ChoreContext context) {
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
