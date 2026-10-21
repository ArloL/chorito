package io.github.arlol.chorito.tools;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class MavenDistributionsTest {

	private static final byte[] DISTRIBUTION = "not really a zip"
			.getBytes(UTF_8);

	private static final String PATH = "/maven2/org/apache/maven/apache-maven/";

	private HttpServer server;
	private final AtomicInteger rateLimited = new AtomicInteger();

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(
				new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
				0
		);
		server.createContext("/maven2/", exchange -> {
			String path = exchange.getRequestURI().getPath();
			String version = path
					.substring(PATH.length(), path.indexOf('/', PATH.length()));
			if (version.equals("limited")
					&& rateLimited.getAndIncrement() < 2) {
				respond(exchange, 429, new byte[0]);
			} else if (version.equals("missing")) {
				respond(exchange, 404, new byte[0]);
			} else if (path.endsWith(".zip")) {
				respond(exchange, 200, DISTRIBUTION);
			} else if (version.equals("tampered")) {
				respond(
						exchange,
						200,
						hex("SHA-512", new byte[0]).getBytes(UTF_8)
				);
			} else {
				respond(
						exchange,
						200,
						(hex("SHA-512", DISTRIBUTION)
								+ "  apache-maven-bin.zip\n").getBytes(UTF_8)
				);
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private Optional<String> lookup(String version) {
		return MavenDistributions.sha256Lookup(
				URI.create(
						"http://localhost:" + server.getAddress().getPort()
								+ "/maven2/"
				)
		).apply(version);
	}

	@Test
	void hashesTheDistribution() {
		assertThat(lookup("3.10.0")).contains(hex("SHA-256", DISTRIBUTION));
	}

	@Test
	void missingDistributionIsEmpty() {
		assertThat(lookup("missing")).isEmpty();
	}

	@Test
	void distributionNotMatchingTheSha512IsEmpty() {
		assertThat(lookup("tampered")).isEmpty();
	}

	@Test
	void retriesWhenRateLimited() {
		assertThat(lookup("limited")).contains(hex("SHA-256", DISTRIBUTION));
	}

	private static void respond(HttpExchange exchange, int status, byte[] body)
			throws IOException {
		exchange.sendResponseHeaders(
				status,
				body.length == 0 ? -1 : body.length
		);
		try (OutputStream out = exchange.getResponseBody()) {
			out.write(body);
		}
	}

	private static String hex(String algorithm, byte[] bytes) {
		try {
			return HexFormat.of()
					.formatHex(
							MessageDigest.getInstance(algorithm).digest(bytes)
					);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

}
