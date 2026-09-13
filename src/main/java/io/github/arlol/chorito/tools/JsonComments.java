package io.github.arlol.chorito.tools;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The comments of a json5 document, kept beside the tree rather than in it.
 *
 * Jackson has no node type for a comment and no slot on {@link ObjectNode} to
 * hang one on, so a comment is stored against the member it belongs to: the
 * object that contains it plus the field name. Keying on the member rather than
 * on the value node is what lets a comment survive a migration that replaces
 * the value under its key, and it avoids the trap that scalar nodes are shared
 * instances - {@code BooleanNode.TRUE}, {@code NullNode.instance} and small
 * {@code IntNode}s are interned, so two unrelated members holding {@code true}
 * are the same object.
 *
 * The containing objects are held by identity. {@link ObjectNode#equals} is by
 * content, which would collapse two members that happen to hold equal objects
 * into one entry.
 *
 * Comment text is stored as it is written in the source, delimiters included,
 * so {@code // why} round-trips as {@code // why}. A block comment keeps its
 * newlines; its lines are de-indented relative to where the comment started so
 * the writer can re-indent it wherever the member ends up.
 *
 * See <a href="https://github.com/FasterXML/jackson-core/issues/734">
 * jackson-core#734</a>: if Jackson ever grows a commentable JsonNode, this
 * class is what it replaces.
 */
public final class JsonComments {

	private static final class Member {

		private final List<String> leading = new ArrayList<>();

		private @Nullable String trailing;

	}

	private final List<String> header = new ArrayList<>();

	private final Map<ObjectNode, Map<String, Member>> members = new IdentityHashMap<>();

	/**
	 * Adds a comment above the root value, before the opening brace.
	 */
	public void addHeader(String comment) {
		header.add(comment);
	}

	public List<String> header() {
		return List.copyOf(header);
	}

	/**
	 * Adds a comment on its own line above the given member.
	 */
	public void addLeading(ObjectNode container, String field, String comment) {
		member(container, field).leading.add(comment);
	}

	/**
	 * Sets the comment that follows the given member on the same line. A member
	 * has at most one.
	 */
	public void setTrailing(
			ObjectNode container,
			String field,
			String comment
	) {
		member(container, field).trailing = comment;
	}

	public List<String> leading(ObjectNode container, String field) {
		Member member = find(container, field);
		return member == null ? List.of() : List.copyOf(member.leading);
	}

	public Optional<String> trailing(ObjectNode container, String field) {
		Member member = find(container, field);
		return member == null ? Optional.empty()
				: Optional.ofNullable(member.trailing);
	}

	private Member member(ObjectNode container, String field) {
		return members.computeIfAbsent(container, key -> new LinkedHashMap<>())
				.computeIfAbsent(field, key -> new Member());
	}

	private @Nullable Member find(ObjectNode container, String field) {
		Map<String, Member> byField = members.get(container);
		return byField == null ? null : byField.get(field);
	}

}
