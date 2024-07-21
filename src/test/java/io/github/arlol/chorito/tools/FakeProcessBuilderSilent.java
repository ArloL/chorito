package io.github.arlol.chorito.tools;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
			Consumer<ProcessBuilderSilent> consumer
	) {
		return (command) -> new FakeProcessBuilderSilent(command, consumer);
	}

	private final String command;
	private final Consumer<ProcessBuilderSilent> consumer;

	public FakeProcessBuilderSilent(
			String[] command,
			Consumer<ProcessBuilderSilent> consumer
	) {
		super(null);
		this.consumer = consumer;
		this.command = Arrays.stream(command).collect(joining(" "));
	}

	@Override
	public FakeProcessBuilderSilent inheritIO() {
		return this;
	}

	@Override
	public FakeProcessSilent start() {
		LOG.info("Would have called {}", command);
		consumer.accept(this);
		return new FakeProcessSilent();
	}

}
