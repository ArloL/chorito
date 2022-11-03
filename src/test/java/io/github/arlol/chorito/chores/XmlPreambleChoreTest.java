package io.github.arlol.chorito.chores;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.arlol.chorito.tools.ChoreContext;
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
		ChoreContext context = extension.choreContext();

		Path pomXml = context.resolve("pom.xml");
		FilesSilent.writeString(pomXml, "<project />");

		new XmlPreambleChore(context.refresh()).doit();

		assertThat(FilesSilent.readString(pomXml)).startsWith(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<project />"
		);
	}

	@Test
	public void testUpdate() throws Exception {
		ChoreContext context = extension.choreContext();

		Path xml = context.resolve("crossdomain.xml");
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

		new XmlPreambleChore(context.refresh()).doit();

		assertThat(FilesSilent.readString(xml)).startsWith(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE"
		);
	}

}
