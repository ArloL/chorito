package io.github.arlol.chorito.chores;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DirectoryStreams;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MavenPlugins;
import io.github.arlol.chorito.tools.MavenPomFile;

public class LifecycleMappingChore implements Chore {

	@Override
	public ChoreContext doit(ChoreContext context) {
		DirectoryStreams.mavenPoms(context).forEach(pomXml -> {
			MavenPomFile pom = MavenPomFile.read(pomXml);
			pom.removePlugin(MavenPlugins.LIFECYCLE_MAPPING);
			FilesSilent.writeString(pomXml, pom.asString());
		});
		return context;
	}

}
