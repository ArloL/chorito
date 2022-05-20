package io.github.arlol.chorito.commands;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class VersionCommand {

	public void execute() {
		try {
			Enumeration<URL> resources = VersionCommand.class.getClassLoader()
					.getResources("META-INF/MANIFEST.MF");
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
	
				Manifest manifest = new Manifest(url.openStream());
				if (isApplicableManifest(manifest)) {
					Attributes attr = manifest.getMainAttributes();
					System.out.println(
							get(attr, "Implementation-Title") + " version \""
									+ get(attr, "Implementation-Version") + "\""
					);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private boolean isApplicableManifest(Manifest manifest) {
		Attributes attributes = manifest.getMainAttributes();
		return "chorito".equals(get(attributes, "Implementation-Title"));
	}

	private static Object get(Attributes attributes, String key) {
		return attributes.get(new Attributes.Name(key));
	}

}
