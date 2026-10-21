package io.github.arlol.chorito.tools;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MavenDistributions {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(5);

	private static final int ATTEMPTS = 3;

	private static final Pattern PUBLISHED_SHA512 = Pattern
			.compile("\\s*([0-9a-fA-F]{128})");

	private MavenDistributions() {
	}

	public static Function<String, Optional<String>> sha256Lookup() {
		return sha256Lookup(
				URI.create("https://repo.maven.apache.org/maven2/")
		);
	}

	/**
	 * The SHA-256 of a Maven version's bin.zip, or empty when it cannot be
	 * downloaded.
	 * <p>
	 * Central publishes SHA-1 and SHA-512 for it but no SHA-256, the one digest
	 * the wrapper checks, so it has to be computed. Downloaded here rather than
	 * by Maven because the chores runner has no JDK. The download is checked
	 * against the published SHA-512, so the SHA-256 is of the distribution
	 * Central actually serves, and retried because Central rate limits.
	 */
	public static Function<String, Optional<String>> sha256Lookup(
			URI repository
	) {
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(CONNECT_TIMEOUT)
				.proxy(ProxySelector.getDefault())
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		return version -> {
			URI zip = repository.resolve(
					"org/apache/maven/apache-maven/" + version
							+ "/apache-maven-" + version + "-bin.zip"
			);
			for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
				Optional<String> sha256 = sha256(client, zip);
				if (sha256.isPresent()) {
					return sha256;
				}
			}
			return Optional.empty();
		};
	}

	private static Optional<String> sha256(HttpClient client, URI zip) {
		try {
			Optional<byte[]> distribution = get(client, zip);
			if (distribution.isEmpty()) {
				return Optional.empty();
			}
			Optional<byte[]> sha512 = get(client, URI.create(zip + ".sha512"));
			if (sha512.isEmpty()) {
				return Optional.empty();
			}
			Matcher published = PUBLISHED_SHA512
					.matcher(new String(sha512.orElseThrow(), UTF_8));
			if (!published.lookingAt()
					|| !hex("SHA-512", distribution.orElseThrow())
							.equalsIgnoreCase(published.group(1))) {
				return Optional.empty();
			}
			return Optional.of(hex("SHA-256", distribution.orElseThrow()));
		} catch (IOException e) {
			return Optional.empty();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	private static Optional<byte[]> get(HttpClient client, URI uri)
			throws IOException, InterruptedException {
		HttpResponse<byte[]> response = client.send(
				HttpRequest.newBuilder(uri)
						.timeout(DOWNLOAD_TIMEOUT)
						.GET()
						.build(),
				HttpResponse.BodyHandlers.ofByteArray()
		);
		if (response.statusCode() != 200) {
			return Optional.empty();
		}
		return Optional.of(response.body());
	}

	private static String hex(String algorithm, byte[] bytes) {
		try {
			return HexFormat.of()
					.formatHex(
							MessageDigest.getInstance(algorithm).digest(bytes)
					);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

}
