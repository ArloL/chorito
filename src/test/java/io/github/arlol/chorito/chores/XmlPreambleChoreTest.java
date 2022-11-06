package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.FileSystemExtension;
import io.github.arlol.chorito.tools.FilesSilent;

public class XmlPreambleChoreTest {

	@RegisterExtension
	final FileSystemExtension extension = new FileSystemExtension();

	@Test
	public void testWithNothing() {
		new XmlPreambleChore(extension.choreContext()).doit();
	}

	@Test
	public void test() throws Exception {
		Path pomXml = extension.root().resolve("pom.xml");
		FilesSilent.writeString(pomXml, "<project />");

		new XmlPreambleChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(pomXml)).startsWith(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project />"
		);
	}

	@Test
	public void testUpdateCrossdomainXml() throws Exception {
		Path xml = extension.root().resolve("crossdomain.xml");
		FilesSilent.writeString(
				xml,
				"""
						<?xml version="1.0"?>
						<!DOCTYPE cross-domain-policy SYSTEM "http://www.adobe.com/xml/dtds/cross-domain-policy.dtd">
						<cross-domain-policy>
						    <!-- Read this: www.adobe.com/devnet/articles/crossdomain_policy_file_spec.html -->

						    <!-- Most restrictive policy: -->
						    <site-control permitted-cross-domain-policies="none"/>

						    <!-- Least restrictive policy: -->
						    <!--
						    <site-control permitted-cross-domain-policies="all"/>
						    <allow-access-from domain="*" to-ports="*" secure="false"/>
						    <allow-http-request-headers-from domain="*" headers="*" secure="false"/>
						    -->
						</cross-domain-policy>
						"""
		);

		new XmlPreambleChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(xml)).startsWith(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE"
		);
	}

	@Test
	public void testUpdateMavenSettingsXml() throws Exception {
		Path xml = extension.root().resolve("settings.xml");
		FilesSilent.writeString(
				xml,
				"""
						<?xml version="1.0" encoding="utf-8"?>
						<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
							xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
							xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
						                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
							<localRepository />
						</settings>
						"""
		);

		new XmlPreambleChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(xml)).startsWith(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<settings"
		);
	}

	@Test
	public void testUpdateInkscapePreferencesXml() throws Exception {
		Path xml = extension.root().resolve("preferences.xml");
		FilesSilent.writeString(
				xml,
				"""
						<?xml version="1.0" encoding="UTF-8" standalone="no"?>
						<inkscape
						   xmlns:sodipodi="http://sodipodi.sourceforge.net/DTD/sodipodi-0.dtd"
						   xmlns:inkscape="http://www.inkscape.org/namespaces/inkscape"
						   version="1">
						"""
		);

		new XmlPreambleChore(extension.choreContext()).doit();

		assertThat(FilesSilent.readString(xml)).startsWith(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<inkscape"
		);
	}

}
