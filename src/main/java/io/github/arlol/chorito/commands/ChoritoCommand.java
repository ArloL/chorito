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
				new CodeQlAnalysisChore(),
				new GitHubActionChore(),
				new GitIgnoreChore(),
				new EclipseCompilerSettingsChore(),
				new JavaUpdaterChore(),
				new JitpackChore(),
				new GraalNativeImagePropertiesChore(),
				new GraalNativeImageMavenPluginMigrationChore(),
				new CodeFormatterProfileChore(),
				new EclipseFormatterPluginChore(),
				new EclipseOptimizeImportSettingsChore(),
				new SpotbugsPluginChore(),
				new ModernizerPluginChore(),
				new LifecycleMappingChore(),
				new VsCodeChore(),
				new IntellijChore(),
				new EditorConfigChore(),
				new Ec4jChore(),
				new RemoveUnnecessaryExecFlagsChore(),
				new DeleteUnnecessaryFilesChore(),
				new DeleteUnwantedFilesChore(),
				new ProhibitedFilenameChore(),
				new IdiomaticVersionFileChore()
		);
		ChoreContext currentContext = GitChoreContext.newBuilder(root).build();
		for (Chore chore : chores) {
			currentContext = chore.doit(currentContext);
			if (currentContext.isDirty()) {
				currentContext = currentContext.refresh();
			}
		}
		currentContext = currentContext.refresh();
		currentContext.deleteIgnoredFiles();
		currentContext.refresh();
	}

}
