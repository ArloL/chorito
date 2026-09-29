package io.github.arlol.chorito.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.arlol.chorito.Main;

class HelpCommandTest {

	@ParameterizedTest
	@ValueSource(strings = { "--help", "-h" })
	void printsUsage(String argument) {
		PrintStream originalOut = System.out;
		var out = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
			Main.main(new String[] { argument });
		} finally {
			System.setOut(originalOut);
		}
		assertThat(out.toString(StandardCharsets.UTF_8))
				.isEqualTo(HelpCommand.USAGE);
	}

}
