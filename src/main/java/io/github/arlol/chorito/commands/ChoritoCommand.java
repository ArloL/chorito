package io.github.arlol.chorito.commands;

import java.util.List;

import io.github.arlol.chorito.chores.*;
import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.GitChoreContext;

public class ChoritoCommand {

	private final String root;

	public ChoritoCommand(String root) {
		this.root = root;
	}

	public void execute() {
		var chores = List.of(
				new GitMasterBranchChore(),
				new ReadmeChore(),
				new GitAttributesChore(),
				new PomParentRelativePathChore(),
				new PomScmChore(),
				new PomPropertiesChore(),
				new LicenseChore(),
				new XmlPreambleChore(),
				new MavenWrapperChore(),
				new GradleWrapperChore(),
				new DockerfileChore(),
				new DockerIgnoreChore(),
				new DependabotChore(),
				new NpmrcChore(),
				new CodeQlAnalysisChore(),
				new GitHubActionChore(),
				new AttestReleaseAssetsChore(),
				new GitIgnoreChore(),
				new EclipseCompilerSettingsChore(),
				new JavaUpdaterChore(),
				new JitpackChore(),
				new GraalNativeImagePropertiesChore(),
				new GraalNativeImageMavenPluginMigrationChore(),
				new CodeFormatterProfileChore(),
				// Formatter, spotbugs, modernizer and source/javadoc form a
				// chain: each inserts its plugin after the one its predecessor
				// inserted, so their relative order here is the only record of
				// that contract. Reordering them, or dropping
				// EclipseFormatterPluginChore, breaks it -- the later chores
				// throw naming the plugin they could not find.
				new EclipseFormatterPluginChore(),
				new EclipseOptimizeImportSettingsChore(),
				new SpotbugsPluginChore(),
				new ModernizerPluginChore(),
				new MavenJavadocSourcesPluginChore(),
				new LifecycleMappingChore(),
				new VsCodeChore(),
				new IntellijChore(),
				new EditorConfigChore(),
				new Ec4jChore(),
				new RemoveUnnecessaryExecFlagsChore(),
				new DeleteUnnecessaryFilesChore(),
				new DeleteUnwantedFilesChore(),
				new ProhibitedFilenameChore(),
				new IdiomaticVersionFileChore(),
				new RenovateChore()
		);
		ChoreContext currentContext = GitChoreContext.newBuilder(root).build();
		for (Chore chore : chores) {
			// Unconditionally, because the alternative was asking each chore
			// to say whether it had created or deleted anything and 22 of the
			// 31 that write files never did. A chore that creates a file
			// without saying so leaves it out of textFiles() for every chore
			// after it -- so the later chore silently does nothing, and fixes
			// itself on the next chorito run, which is the hardest kind of bug
			// to attribute. Re-scanning costs about 20ms and buys correctness
			// that does not depend on 40 authors in sequence remembering a
			// rule.
			currentContext = chore.doit(currentContext).refresh();
		}
		currentContext.deleteIgnoredFiles();
		currentContext.refresh();
	}

}
