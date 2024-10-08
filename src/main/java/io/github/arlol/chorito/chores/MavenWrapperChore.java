package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.ExecutableFlagger;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.MyPaths;

public class MavenWrapperChore implements Chore {

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
			#   http://www.apache.org/licenses/LICENSE-2.0
			#
			# Unless required by applicable law or agreed to in writing,
			# software distributed under the License is distributed on an
			# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
			# KIND, either express or implied.  See the License for the
			# specific language governing permissions and limitations
			# under the License.
			wrapperVersion=3.3.2
			distributionType=bin
			distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.8/apache-maven-3.9.8-bin.zip
			wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar
			""";

	@Override
	public ChoreContext doit(ChoreContext context) {
		LOG.info("Running MavenWrapperChore");
		DirectoryStreams.rootMavenPoms(context)
				.map(MyPaths::getParent)
				.forEach(pomDir -> {
					Path wrapper = pomDir.resolve("mvnw");
					Path wrapperJar = pomDir
							.resolve(".mvn/wrapper/maven-wrapper.jar");
					Path wrapperProperties = pomDir
							.resolve(".mvn/wrapper/maven-wrapper.properties");
					if (!FilesSilent.exists(wrapper)
							|| !FilesSilent.exists(wrapperJar)) {
						LOG.info("Running mvn wrapper:3.3.2:wrapper");
						context.newProcessBuilder(
								"mvn",
								"-N",
								"wrapper:3.3.2:wrapper",
								"-Dmaven=3.9.8",
								"-Dtype=bin"
						)
								.inheritIO()
								.directory(pomDir)
								.start()
								.waitFor(5, TimeUnit.MINUTES);
						context.setDirty();
					}
					if (!FilesSilent.exists(wrapperJar)) {
						throw new IllegalStateException("No maven-wrapper.jar");
					}
					ExecutableFlagger.makeExecutableIfPossible(wrapper);
					if (FilesSilent.exists(wrapperProperties)) {
						String content = FilesSilent
								.readString(wrapperProperties);
						if (!DEFAULT_PROPERTIES.equals(content)) {
							LOG.info("Running ./mvnw wrapper:3.3.2:wrapper");
							context.newProcessBuilder(
									"./mvnw",
									"-N",
									"wrapper:3.3.2:wrapper",
									"-Dmaven=3.9.8",
									"-Dtype=bin"
							)
									.inheritIO()
									.directory(pomDir)
									.start()
									.waitFor(5, TimeUnit.MINUTES);
							context.setDirty();
						}
					}
				});
		return context;
	}

}
