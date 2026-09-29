package io.github.arlol.chorito.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class GitHubRepositoriesTest {

	private HttpServer server;
	private final AtomicInteger requests = new AtomicInteger();

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(
				new InetSocketAddress(InetAddress.getLoopbackAddress(), 0),
				0
		);
		server.createContext("/repos/", exchange -> {
			requests.incrementAndGet();
			String path = exchange.getRequestURI().getPath();
			int status = 200;
			String body = switch (path) {
			case "/repos/owner/public" -> "{\"private\":false}";
			case "/repos/owner/private" -> "{\"private\":true}";
			case "/repos/owner/garbled" -> "not json";
			case "/repos/owner/limited" -> {
				status = 403;
				yield "{\"message\":\"API rate limit exceeded\"}";
			}
			default -> {
				status = 404;
				yield "{\"message\":\"Not Found\"}";
			}
			};
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status, bytes.length);
			try (OutputStream out = exchange.getResponseBody()) {
				out.write(bytes);
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	private Predicate<String> lookup() {
		return GitHubRepositories.publicLookup(
				URI.create(
						"http://localhost:" + server.getAddress().getPort()
								+ "/"
				)
		);
	}

	@Test
	void publicRepositoryIsPublic() {
		assertThat(lookup().test("owner/public")).isTrue();
	}

	@Test
	void privateRepositoryIsNotPublic() {
		assertThat(lookup().test("owner/private")).isFalse();
	}

	@Test
	void notFoundIsNotPublic() {
		assertThat(lookup().test("owner/missing")).isFalse();
	}

	@Test
	void rateLimitIsNotPublic() {
		assertThat(lookup().test("owner/limited")).isFalse();
	}

	@Test
	void unreadableAnswerIsNotPublic() {
		assertThat(lookup().test("owner/garbled")).isFalse();
	}

	@Test
	void unreachableApiIsNotPublic() {
		Predicate<String> lookup = lookup();
		server.stop(0);

		assertThat(lookup.test("owner/public")).isFalse();
	}

	@Test
	void eachRepositoryIsAskedAboutOnce() {
		Predicate<String> lookup = lookup();

		lookup.test("owner/public");
		lookup.test("owner/public");

		assertThat(requests).hasValue(1);
	}

	@Test
	void nameOfGitHubRemotes() {
		assertThat(
				GitHubRepositories.nameOf("https://github.com/ArloL/chorito")
		).hasValue("ArloL/chorito");
		assertThat(
				GitHubRepositories
						.nameOf("https://github.com/ArloL/chorito.git")
		).hasValue("ArloL/chorito");
		assertThat(
				GitHubRepositories.nameOf("https://github.com/ArloL/chorito/")
		).hasValue("ArloL/chorito");
		assertThat(
				GitHubRepositories.nameOf("https://github.com/ArloL/chorito.js")
		).hasValue("ArloL/chorito.js");
	}

	@Test
	void nameOfOtherRemotes() {
		assertThat(
				GitHubRepositories.nameOf("https://gitlab.com/ArloL/chorito")
		).isEmpty();
		assertThat(
				GitHubRepositories.nameOf("git@github.com:ArloL/chorito.git")
		).isEmpty();
		assertThat(GitHubRepositories.nameOf("https://github.com/ArloL"))
				.isEmpty();
		assertThat(
				GitHubRepositories
						.nameOf("https://github.com/ArloL/chorito/tree/main")
		).isEmpty();
	}

}
