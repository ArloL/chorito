package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.FilesSilent;

public class MavenWrapperChore {

	private static Logger LOG = LoggerFactory
			.getLogger(MavenWrapperChore.class);

	private static String DEFAULT_PROPERTIES = """
			# Licensed to the Apache Software Foundation (ASF) under one
			# or more contributor license agreements.  See the NOTICE file
			# distributed with this work for additional information
			# regarding copyright ownership.  The ASF licenses this file
			# to you under the Apache License, Version 2.0 (the
			# "License"); you may not use this file except in compliance
			# with the License.  You may obtain a copy of the License at
			#
			#   https://www.apache.org/licenses/LICENSE-2.0
			#
			# Unless required by applicable law or agreed to in writing,
			# software distributed under the License is distributed on an
			# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
			# KIND, either express or implied.  See the License for the
			# specific language governing permissions and limitations
			# under the License.
			distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.8.6/apache-maven-3.8.6-bin.zip
			wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.1.1/maven-wrapper-3.1.1.jar
			""";

	private final ChoreContext context;

	public MavenWrapperChore(ChoreContext context) {
		this.context = context;
	}

	public void doit() {
		LOG.info("Running MavenWrapperChore");
		Path pom = context.resolve("pom.xml");
		if (!FilesSilent.exists(pom)) {
			return;
		}
		Path wrapper = context.resolve("mvnw");
		if (!FilesSilent.exists(wrapper)) {
			LOG.info("Running mvn wrapper:wrapper");
			context.newProcessBuilder(
					"mvn",
					"-N",
					"wrapper:wrapper",
					"-Dmaven=3.8.6"
			).inheritIO().start().waitFor(5, TimeUnit.MINUTES);
		}
		if (!FilesSilent
				.exists(context.resolve(".mvn/wrapper/maven-wrapper.jar"))) {
			throw new IllegalStateException("No maven-wrapper.jar");
		}
		var permissions = FilesSilent.getPosixFilePermissions(wrapper);
		permissions.add(PosixFilePermission.OWNER_EXECUTE);
		permissions.add(PosixFilePermission.GROUP_EXECUTE);
		permissions.add(PosixFilePermission.OTHERS_EXECUTE);
		FilesSilent.setPosixFilePermissions(wrapper, permissions);
		Path path = context.resolve(".mvn/wrapper/maven-wrapper.properties");
		if (FilesSilent.exists(path)) {
			String content = FilesSilent.readString(path);
			if (!DEFAULT_PROPERTIES.equals(content)) {
				LOG.info("Running ./mvnw wrapper:wrapper");
				context.newProcessBuilder(
						"./mvnw",
						"-N",
						"wrapper:wrapper",
						"-Dmaven=3.8.6"
				).inheritIO().start().waitFor(5, TimeUnit.MINUTES);
			}
		}
	}

}
