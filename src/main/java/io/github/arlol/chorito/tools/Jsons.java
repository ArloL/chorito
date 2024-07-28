package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.core.util.Separators.Spacing;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public abstract class Jsons {

	private Jsons() {
	}

	public static ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.setConfig(
				objectMapper.getSerializationConfig()
						.withDefaultPrettyPrinter(
								new CustomPrettyPrinter().withSeparators(
										new Separators(
												Separators.DEFAULT_ROOT_VALUE_SEPARATOR,
												':',
												Spacing.AFTER,
												',',
												Spacing.NONE,
												Separators.DEFAULT_OBJECT_EMPTY_SEPARATOR,
												',',
												Spacing.NONE,
												Separators.DEFAULT_ARRAY_EMPTY_SEPARATOR
										)
								)
						)
		);
		return objectMapper;
	}

	public static String asString(Object object) {
		try {
			return objectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Optional<JsonNode> parse(Path extensions) {
		try {
			return Optional.ofNullable(
					objectMapper().readTree(Files.newInputStream(extensions))
			);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
