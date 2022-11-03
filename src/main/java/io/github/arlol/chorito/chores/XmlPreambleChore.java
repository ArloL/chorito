package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class XmlPreambleChore {

	private static final String XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

	private final ChoreContext context;

	public XmlPreambleChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		context.textFiles()
				.stream()
				.filter(path -> path.toString().endsWith(".xml"))
				.forEach(path -> {
					String content = FilesSilent.readString(path);
					if (!content.startsWith(XML_PREAMBLE)) {
						content = XML_PREAMBLE + content;
						FilesSilent.writeString(path, content);
					}
				});
	}

}
