package io.github.arlol.chorito.chores;

import java.nio.file.Path;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.JsoupSilent;

public class PomScmChore {

	private ChoreContext context;

	public PomScmChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		Path pom = context.resolve("pom.xml");
		if (context.hasGitHubRemote() && context.remotes().size() == 1
				&& FilesSilent.exists(pom)) {
			String remote = context.remotes().get(0);

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
					url.text(remote.replace(".git", "/"));
				}
			}
			FilesSilent.writeString(pom, doc.outerHtml());
		}
	}

}
