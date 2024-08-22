package io.github.arlol.chorito.tools;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

public class CustomPrettyPrinter extends DefaultPrettyPrinter {

	private static final long serialVersionUID = 1L;

	public CustomPrettyPrinter(Separators separators) {
		super(separators);
		DefaultIndenter defaultIndenter = new DefaultIndenter("    ", "\n");
		indentArraysWith(defaultIndenter);
		indentObjectsWith(defaultIndenter);
	}

	public CustomPrettyPrinter(CustomPrettyPrinter customPrettyPrinter) {
		super(customPrettyPrinter);
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
		}
		super.writeEndObject(g, nrOfEntries);
	}

	@Override
	public DefaultPrettyPrinter createInstance() {
		return new CustomPrettyPrinter(this);
	}

}
