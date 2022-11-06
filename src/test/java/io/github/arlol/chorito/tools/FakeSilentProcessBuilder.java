package io.github.arlol.chorito.tools;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeSilentProcessBuilder extends SilentProcessBuilder {

	private static Logger LOG = LoggerFactory
			.getLogger(FakeSilentProcessBuilder.class);

	public static class FakeSilentProcess extends SilentProcess {

		public FakeSilentProcess() {
			super(null);
		}

		@Override
		public void waitFor(int timeout, TimeUnit unit) {
		}

	}

	public static Function<String[], SilentProcessBuilder> factory(
			Runnable... runnables
	) {
		return (command) -> new FakeSilentProcessBuilder(command, runnables);
	}

	private final String command;
	private final Runnable[] runnables;

	public FakeSilentProcessBuilder(String[] command, Runnable... runnables) {
		super(null);
		this.runnables = runnables;
		this.command = Arrays.stream(command).collect(joining(" "));
	}

	@Override
	public SilentProcessBuilder inheritIO() {
		return this;
	}

	@Override
	public SilentProcess start() {
		LOG.info("Would have called {}", command);
		Arrays.stream(runnables).forEach(Runnable::run);
		return new FakeSilentProcess();
	}

}
