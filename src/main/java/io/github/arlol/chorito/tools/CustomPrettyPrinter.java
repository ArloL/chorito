package io.github.arlol.chorito.tools;

import java.io.IOException;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

/**
 * Lays out json the way chorito writes it, and emits the comments Jackson
 * itself cannot.
 *
 * A comment has to land between the entry separator and the indentation of the
 * entry it belongs to, and only the printer is called at that point. The
 * printer does not know which member is being written, so the caller pushes the
 * comments in with {@link #pending} right before {@code writeFieldName}; the
 * next hook writes and clears them. That makes the printer stateful and
 * single-use, which is why {@link Jsons#prettyPrinter()} hands out a new one
 * per call.
 *
 * Jackson 3.2 adds {@code JsonGenerator.writeComment}, which would replace the
 * raw writes below.
 */
public class CustomPrettyPrinter extends DefaultPrettyPrinter {

	private static final long serialVersionUID = 1L;

	/**
	 * The comments waiting to be written, each line of them written at the
	 * indentation of the entry they belong to. Several leading comments are
	 * held as one newline joined string because that is how they come out
	 * again, and because a String keeps this Serializable class free of fields
	 * that would not survive a round trip.
	 */
	private @Nullable String pendingLeading;

	private @Nullable String pendingTrailing;

	public CustomPrettyPrinter(Separators separators) {
		super(separators);
		DefaultIndenter defaultIndenter = new DefaultIndenter("    ", "\n");
		indentArraysWith(defaultIndenter);
		indentObjectsWith(defaultIndenter);
	}

	public CustomPrettyPrinter(CustomPrettyPrinter customPrettyPrinter) {
		super(customPrettyPrinter);
	}

	/**
	 * Hands the printer the comments of the member about to be written and the
	 * trailing comment of the one just finished. Both are written and cleared
	 * by the next entry separator or end of object.
	 */
	public void pending(List<String> leading, @Nullable String trailing) {
		pendingLeading = leading.isEmpty() ? null : String.join("\n", leading);
		pendingTrailing = trailing;
	}

	@Override
	public void beforeObjectEntries(JsonGenerator g) throws IOException {
		writePendingLeading(g);
		super.beforeObjectEntries(g);
	}

	@Override
	public void writeObjectEntrySeparator(JsonGenerator g) throws IOException {
		g.writeRaw(_objectEntrySeparator);
		writePendingTrailing(g);
		writePendingLeading(g);
		_objectIndenter.writeIndentation(g, _nesting);
	}

	@Override
	public void writeEndArray(JsonGenerator g, int nrOfValues)
			throws IOException {
		if (nrOfValues > 0) {
			g.writeRaw(_arrayValueSeparator);
		}
		super.writeEndArray(g, nrOfValues);
	}

	@Override
	public void writeEndObject(JsonGenerator g, int nrOfEntries)
			throws IOException {
		if (nrOfEntries > 0) {
			g.writeRaw(_objectEntrySeparator);
			writePendingTrailing(g);
		}
		super.writeEndObject(g, nrOfEntries);
	}

	@Override
	public DefaultPrettyPrinter createInstance() {
		return new CustomPrettyPrinter(this);
	}

	private void writePendingLeading(JsonGenerator g) throws IOException {
		String comments = pendingLeading;
		pendingLeading = null;
		if (comments == null) {
			return;
		}
		for (String line : comments.lines().toList()) {
			_objectIndenter.writeIndentation(g, _nesting);
			g.writeRaw(line);
		}
	}

	private void writePendingTrailing(JsonGenerator g) throws IOException {
		String comment = pendingTrailing;
		pendingTrailing = null;
		if (comment == null) {
			return;
		}
		boolean first = true;
		for (String line : comment.lines().toList()) {
			if (first) {
				g.writeRaw(' ');
				first = false;
			} else {
				_objectIndenter.writeIndentation(g, _nesting);
			}
			g.writeRaw(line);
		}
	}

}
