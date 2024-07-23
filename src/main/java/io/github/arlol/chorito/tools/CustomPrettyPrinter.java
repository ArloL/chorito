package io.github.arlol.chorito.tools;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

public class CustomPrettyPrinter extends DefaultPrettyPrinter {

	private static final long serialVersionUID = 1L;

	public CustomPrettyPrinter() {
		_arrayIndenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
		_objectIndenter = new DefaultIndenter("    ", DefaultIndenter.SYS_LF);
	}

	@Override
	public void writeStartObject(JsonGenerator g) throws IOException {
		super.writeStartObject(g);
	}

	@Override
	public void beforeObjectEntries(JsonGenerator g) throws IOException {
		super.beforeObjectEntries(g);
	}

	@Override
	public void writeEndObject(JsonGenerator g, int nrOfEntries)
			throws IOException {
		super.writeEndObject(g, nrOfEntries);
	}

	@Override
	public void writeStartArray(JsonGenerator g) throws IOException {
		super.writeStartArray(g);
	}

	@Override
	public void beforeArrayValues(JsonGenerator g) throws IOException {
		super.beforeArrayValues(g);
	}

	@Override
	public void writeEndArray(JsonGenerator g, int nrOfValues)
			throws IOException {
		super.writeEndArray(g, nrOfValues);
	}

	@Override
	public CustomPrettyPrinter createInstance() {
		return new CustomPrettyPrinter();
	}

}
