package io.github.arlol.chorito.commands;

public class HelpCommand {

	static final String USAGE = """
			Usage: chorito [<path>]
			       chorito --version
			       chorito --help

			Does some chores in the git repository at <path>, or in the
			current directory if no path is given.

			Options:
			  -h, --help     Print this help and exit.
			  --version      Print the version and exit.
			""";

	public void execute() {
		System.out.print(USAGE);
	}

}
