package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

public class JsoupSilent {

	public static Document parse(
			Path path,
			@Nullable String charsetName,
			String baseUri,
			Parser parser
	) {
		try (InputStream inputStream = Files.newInputStream(path)) {
			return Jsoup.parse(inputStream, charsetName, baseUri, parser);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
