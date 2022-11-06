package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

public class SilentProcessBuilder {

	public static class SilentProcess {

		private final Process process;

		public SilentProcess(Process process) {
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

	public static SilentProcessBuilder create(String... command) {
		return create(new ProcessBuilder(command));
	}

	public static SilentProcessBuilder create(ProcessBuilder processBuilder) {
		return new SilentProcessBuilder(processBuilder);
	}

	private final ProcessBuilder processBuilder;

	protected SilentProcessBuilder(ProcessBuilder processBuilder) {
		this.processBuilder = processBuilder;
	}

	public ProcessBuilder processBuilder() {
		return processBuilder;
	}

	public SilentProcessBuilder inheritIO() {
		processBuilder.inheritIO();
		return this;
	}

	public SilentProcess start() {
		try {
			return new SilentProcess(processBuilder.start());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
