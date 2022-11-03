package io.github.arlol.chorito.chores;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

import java.util.Arrays;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class XmlPreambleChore {

	private static final String XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

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
					if (!content.startsWith("<?xml ")) {
						FilesSilent.writeString(path, XML_PREAMBLE + content);
					} else {
						int endOfPreamble = content.indexOf("?>\n");
						String contentOfPreamble = content
								.substring("<?xml ".length(), endOfPreamble);
						var attributes = Arrays
								.stream(contentOfPreamble.split(" "))
								.map(arg -> arg.replace("\"", ""))
								.map(arg -> arg.split("=", 2))
								.collect(
										toMap(
												value -> value[0],
												value -> value[1]
										)
								);
						attributes.putIfAbsent("encoding", "UTF-8");
						attributes.remove("version");
						String newPreamble = attributes.entrySet()
								.stream()
								.map(entry -> {
									if (entry.getKey().equals("encoding")
											&& entry.getValue()
													.equalsIgnoreCase(
															"UTF-8"
													)) {
										entry.setValue("UTF-8");
									}
									return entry;
								})
								.map(
										entry -> entry.getKey() + "=\""
												+ entry.getValue() + "\""
								)
								.sorted()
								.collect(
										joining(
												" ",
												"<?xml version=\"1.0\" ",
												"?>\n"
										)
								);

						String contentNoPreamble = content
								.substring(endOfPreamble + "?>\n".length());
						FilesSilent.writeString(
								path,
								newPreamble + contentNoPreamble
						);
					}
				});
	}

}
