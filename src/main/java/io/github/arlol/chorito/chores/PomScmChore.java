package io.github.arlol.chorito.chores;

import java.util.stream.Collectors;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class PomScmChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		context.remotes()
				.stream()
				.filter(s -> s.startsWith("https://github.com"))
				.map(s -> s.replace(".git", ""))
				// if there are multiple return null
				.collect(Collectors.reducing((_, _) -> null))
				.ifPresent(remote -> {

					DirectoryStreams.rootMavenPoms(context).forEach(pom -> {
						Document doc = JsoupSilent
								.parse(pom, "UTF-8", "", Parser.xmlParser());
						Element scm = doc.selectFirst("project > scm");
						if (scm != null) {
							Element connection = scm.selectFirst("connection");
							if (connection != null) {
								connection.text("scm:git:" + remote);
							}
							Element developerConnection = scm
									.selectFirst("developerConnection");
							if (developerConnection != null) {
								developerConnection.text("scm:git:" + remote);
							}
							Element url = scm.selectFirst("url");
							if (url != null) {
								url.text(remote);
							}
						}
						FilesSilent.writeString(pom, doc.outerHtml());

					});
				});
		return context;
	}

}
