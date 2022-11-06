package io.github.arlol.chorito.tools;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FakeProcessBuilderSilent extends ProcessBuilderSilent {

	private static Logger LOG = LoggerFactory
			.getLogger(FakeProcessBuilderSilent.class);

	public static class FakeProcessSilent extends ProcessSilent {

		public FakeProcessSilent() {
			super(null);
		}

		@Override
		public void waitFor(int timeout, TimeUnit unit) {
		}

	}

	public static Function<String[], ProcessBuilderSilent> factory(
			Runnable... runnables
	) {
		return (command) -> new FakeProcessBuilderSilent(command, runnables);
	}

	private final String command;
	private final Runnable[] runnables;

	public FakeProcessBuilderSilent(String[] command, Runnable... runnables) {
		super(null);
		this.runnables = runnables;
		this.command = Arrays.stream(command).collect(joining(" "));
	}

	@Override
	public FakeProcessBuilderSilent inheritIO() {
		return this;
	}

	@Override
	public FakeProcessSilent start() {
		LOG.info("Would have called {}", command);
		Arrays.stream(runnables).forEach(Runnable::run);
		return new FakeProcessSilent();
	}

}
