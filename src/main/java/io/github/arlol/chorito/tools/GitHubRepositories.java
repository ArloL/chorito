package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

public final class GitHubRepositories {

	private static final Pattern REMOTE = Pattern.compile(
			"https://github\\.com/([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+?)(?:\\.git)?/?"
	);

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private GitHubRepositories() {
	}

	/**
	 * The {@code owner/repo} a remote points at, or empty when it is not an
	 * https remote on github.com.
	 */
	public static Optional<String> nameOf(String remote) {
		Matcher matcher = REMOTE.matcher(remote);
		if (!matcher.matches()) {
			return Optional.empty();
		}
		return Optional.of(matcher.group(1));
	}

	public static Predicate<String> publicLookup() {
		return publicLookup(URI.create("https://api.github.com/"));
	}

	/**
	 * Asks the GitHub API, without a token, whether a repository is public.
	 * <p>
	 * Without a token a private repository answers 404, the same as one that
	 * does not exist, and the limit is 60 calls an hour. Every answer but a 200
	 * saying {@code "private": false} -- including a network error or the rate
	 * limit -- counts as not public, so the chores that depend on it skip
	 * rather than put an MIT licence on client code. {@code git ls-remote}
	 * cannot stand in for this: a credential helper signs it in and private
	 * repositories answer like public ones.
	 * <p>
	 * Each repository is asked about once. The predicate travels with every
	 * refreshed {@link ChoreContext}, so all chores of a run share the answer.
	 */
	public static Predicate<String> publicLookup(URI apiBase) {
		HttpClient client = HttpClient.newBuilder()
				.connectTimeout(TIMEOUT)
				.proxy(ProxySelector.getDefault())
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		Map<String, Boolean> answers = new ConcurrentHashMap<>();
		return name -> answers
				.computeIfAbsent(name, n -> isPublic(client, apiBase, n));
	}

	private static boolean isPublic(
			HttpClient client,
			URI apiBase,
			String name
	) {
		HttpRequest request = HttpRequest
				.newBuilder(apiBase.resolve("repos/" + name))
				.timeout(TIMEOUT)
				.header("Accept", "application/vnd.github+json")
				.GET()
				.build();
		try {
			HttpResponse<String> response = client
					.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				return false;
			}
			JsonNode isPrivate = Jsons.objectMapper()
					.readTree(response.body())
					.path("private");
			return isPrivate.isBoolean() && !isPrivate.booleanValue();
		} catch (IOException | JacksonException e) {
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

}
