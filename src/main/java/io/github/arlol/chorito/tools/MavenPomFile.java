package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;

import io.github.arlol.chorito.tools.MavenPlugins.Id;

/**
 * A Maven pom, in the terms the chores actually work in -- plugins located by
 * coordinates rather than by CSS selector.
 * <p>
 * It exists so jsoup stays on this side of the boundary, the way snakeyaml
 * stays behind {@link GitHubActionsWorkflowFile} and Jackson behind
 * {@link Jsons}. Before it, the rule for finding a plugin was a selector string
 * written out twelve times across seven chores; changing how plugins are found
 * meant changing all twelve.
 */
public class MavenPomFile {

	private final Document document;

	public MavenPomFile(String xml) {
		this.document = Jsoup.parse(xml, "", Parser.xmlParser());
	}

	private MavenPomFile(Document document) {
		this.document = document;
	}

	public static MavenPomFile read(Path path) {
		return new MavenPomFile(
				JsoupSilent.parse(path, "UTF-8", "", Parser.xmlParser())
		);
	}

	public boolean hasPlugin(Id plugin) {
		return findPlugin(plugin) != null;
	}

	/**
	 * The version {@code plugin} is declared at, empty when the plugin is
	 * absent or inherits its version from a parent pom.
	 */
	public Optional<String> pluginVersion(Id plugin) {
		Element element = findPlugin(plugin);
		if (element == null) {
			return Optional.empty();
		}
		Element version = element.selectFirst("> version");
		return Optional.ofNullable(version).map(Element::ownText);
	}

	/**
	 * The own text of the element {@code path} names below {@code plugin}'s
	 * {@code <configuration>}, empty when the plugin or any step of the path is
	 * absent.
	 */
	public Optional<String> configuration(Id plugin, String... path) {
		return Optional.ofNullable(findConfiguration(plugin, path))
				.map(Element::ownText);
	}

	/**
	 * Writes {@code value} into the element {@code path} names below
	 * {@code plugin}'s {@code <configuration>}, and does nothing when that
	 * element is absent -- a pom that does not configure something has not
	 * asked for chorito's value of it.
	 */
	public void setConfiguration(Id plugin, String value, String... path) {
		Element element = findConfiguration(plugin, path);
		if (element != null) {
			element.text(value);
		}
	}

	/**
	 * Inserts {@code pluginXml} immediately after {@code anchor}, which must be
	 * declared in this pom.
	 */
	public void insertPluginAfter(Id anchor, String pluginXml) {
		Element element = findPlugin(anchor);
		if (element == null) {
			throw new IllegalStateException("No " + anchor + " plugin");
		}
		element.after(pluginXml);
	}

	/**
	 * Inserts {@code pluginXml} after the last plugin declared anywhere in this
	 * pom -- the fallback for a pom that declares none of the plugins a chore
	 * would rather anchor against.
	 */
	public void insertPluginAfterLastPlugin(String pluginXml) {
		Element element = document.select("plugin").last();
		if (element == null) {
			throw new IllegalStateException("No plugin");
		}
		element.after(pluginXml);
	}

	/**
	 * Removes every declaration of {@code plugin}, along with the whitespace
	 * and comments in front of it, so the surrounding pom keeps its
	 * indentation.
	 */
	public void removePlugin(Id plugin) {
		for (Element element : document.select(selector(plugin))) {
			while (!(element.previousSibling() instanceof Element)) {
				Node previousSibling = element.previousSibling();
				if (previousSibling == null) {
					break;
				}
				previousSibling.remove();
			}
			element.remove();
		}
	}

	/**
	 * Adds a {@code <property>} named {@code propertyName} to the activation of
	 * every profile whose {@code <file><exists>} ends with
	 * {@code fileExistsSuffix}, so Maven requires both conditions. The new
	 * element copies the indentation of the {@code <file>} it follows.
	 *
	 * @return whether the pom changed
	 */
	public boolean addProfileActivationProperty(
			String fileExistsSuffix,
			String propertyName
	) {
		boolean changed = false;
		for (Element file : document
				.select("profile > activation > file:has(> exists)")) {
			Element exists = file.selectFirst("> exists");
			Element activation = file.parent();
			if (exists == null || activation == null
					|| !exists.ownText().trim().endsWith(fileExistsSuffix)
					|| hasProperty(activation, propertyName)) {
				continue;
			}
			String indent = whitespaceBefore(file);
			String existsIndent = whitespaceBefore(exists);
			String step = existsIndent.startsWith(indent)
					&& existsIndent.length() > indent.length()
							? existsIndent.substring(indent.length())
							: "\t";
			file.after(
					indent + "<property>" + indent + step + "<name>"
							+ propertyName + "</name>" + indent + "</property>"
			);
			changed = true;
		}
		return changed;
	}

	private static boolean hasProperty(Element activation, String name) {
		return activation.select("> property > name")
				.stream()
				.anyMatch(n -> n.ownText().trim().equals(name));
	}

	private static String whitespaceBefore(Element element) {
		return element.previousSibling() instanceof TextNode text
				&& text.isBlank() ? text.getWholeText() : "";
	}

	public String asString() {
		return document.outerHtml();
	}

	@Nullable
	private Element findConfiguration(Id plugin, String... path) {
		Element element = findPlugin(plugin);
		if (element != null) {
			element = element.selectFirst("> configuration");
		}
		for (String step : path) {
			if (element == null) {
				return null;
			}
			element = element.selectFirst("> " + step);
		}
		return element;
	}

	private Element findPlugin(Id plugin) {
		return document.select(selector(plugin)).first();
	}

	private static String selector(Id plugin) {
		return "plugin:has(groupId:containsWholeOwnText(" + plugin.groupId()
				+ ")):has(artifactId:containsWholeOwnText("
				+ plugin.artifactId() + "))";
	}

}
