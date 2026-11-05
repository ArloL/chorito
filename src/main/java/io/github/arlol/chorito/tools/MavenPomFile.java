package io.github.arlol.chorito.tools;

import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nullable;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
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
	 * Gives every {@code <profile>} that declares {@code plugin} exactly this
	 * activation, whatever it had before, including none: a
	 * {@code <file><exists>} of {@code fileExists} and a {@code <property>}
	 * named {@code propertyName}. A profile that already has it, compared by
	 * structure, is left byte-identical.
	 *
	 * @return whether the pom changed
	 */
	public boolean setActivationOfProfilesWithPlugin(
			Id plugin,
			String fileExists,
			String propertyName
	) {
		boolean changed = false;
		for (Element profile : document
				.select("profile:has(" + selector(plugin) + ")")) {
			Element activation = profile.selectFirst("> activation");
			if (activation != null
					&& hasActivation(activation, fileExists, propertyName)) {
				continue;
			}
			if (activation != null) {
				String indent = whitespaceBefore(activation);
				activation
						.html(activationXml(indent, fileExists, propertyName));
			} else {
				Element id = profile.selectFirst("> id");
				if (id == null) {
					continue;
				}
				String indent = whitespaceBefore(id);
				id.after(
						indent + "<activation>"
								+ activationXml(
										indent,
										fileExists,
										propertyName
								) + "</activation>"
				);
			}
			changed = true;
		}
		return changed;
	}

	private static String activationXml(
			String indent,
			String fileExists,
			String propertyName
	) {
		String step = "\t";
		return indent + step + "<file>" + indent + step + step + "<exists>"
				+ fileExists + "</exists>" + indent + step + "</file>" + indent
				+ step + "<property>" + indent + step + step + "<name>"
				+ propertyName + "</name>" + indent + step + "</property>"
				+ indent;
	}

	private static boolean hasActivation(
			Element activation,
			String fileExists,
			String propertyName
	) {
		Elements children = activation.children();
		if (children.size() != 2) {
			return false;
		}
		Element file = children.get(0);
		Element property = children.get(1);
		return file.tagName().equals("file") && file.children().size() == 1
				&& file.child(0).tagName().equals("exists")
				&& file.child(0).ownText().trim().equals(fileExists)
				&& property.tagName().equals("property")
				&& property.children().size() == 1
				&& property.child(0).tagName().equals("name")
				&& property.child(0).ownText().trim().equals(propertyName);
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
