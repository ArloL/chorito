package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class PropertiesSilent {

	private final Properties properties = new Properties();

	public PropertiesSilent load(Path path) {
		try (InputStream inputStream = Files.newInputStream(path)) {
			properties.load(inputStream);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return this;
	}

	public PropertiesSilent load(Reader reader) {
		try {
			properties.load(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return this;
	}

	public Map<String, String> toMap() {
		Map<String, String> result = new TreeMap<>();
		properties.stringPropertyNames().forEach(key -> {
			result.put(key, properties.getProperty(key));
		});
		return result;
	}

}
