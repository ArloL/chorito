package io.github.arlol.chorito;

import io.github.arlol.chorito.commands.ChoritoCommand;
import io.github.arlol.chorito.commands.VersionCommand;

public class Main {

	public static void main(String[] args) {
		if (args.length == 1 && "--version".equals(args[0])) {
			new VersionCommand().execute();
			return;
		}

		String pathArgument;
		if (args.length > 0) {
			pathArgument = args[0];
		} else {
			pathArgument = "";
		}

		new ChoritoCommand(pathArgument).execute();
	}

}
