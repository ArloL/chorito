package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ProcessBuilderSilent {

	public static class ProcessSilent {

		private final Process process;

		public ProcessSilent(Process process) {
			this.process = process;
		}

		public Process process() {
			return process;
		}

		public void waitFor(int timeout, TimeUnit unit) {
			try {
				process.waitFor(timeout, unit);
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		}

	}

	public static Function<String[], ProcessBuilderSilent> factory() {
		return (command) -> ProcessBuilderSilent.create(command);
	}

	public static ProcessBuilderSilent create(String... command) {
		return create(new ProcessBuilder(command));
	}

	public static ProcessBuilderSilent create(ProcessBuilder processBuilder) {
		return new ProcessBuilderSilent(processBuilder);
	}

	private final ProcessBuilder processBuilder;
	private Path directory;

	protected ProcessBuilderSilent(ProcessBuilder processBuilder) {
		this.processBuilder = processBuilder;
	}

	public ProcessBuilder processBuilder() {
		return processBuilder;
	}

	public ProcessBuilderSilent inheritIO() {
		processBuilder.inheritIO();
		return this;
	}

	public ProcessBuilderSilent directory(Path directory) {
		this.directory = directory;
		return this;
	}

	public Path directory() {
		return directory;
	}

	public ProcessSilent start() {
		try {
			if (directory != null) {
				processBuilder.directory(directory.toFile());
			}
			return new ProcessSilent(processBuilder.start());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
