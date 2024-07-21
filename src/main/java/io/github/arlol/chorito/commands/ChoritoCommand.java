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
		List<Chore> chores = List.of(
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
				new JavaUpdaterChore(),
				new JitpackChore(),
				new GraalNativeImagePropertiesChore(),
				new GraalNativeImageMavenPluginMigrationChore(),
				new CodeFormatterProfileChore(),
				new EclipseFormatterPluginChore(),
				new EclipseCompilerSettingsChore(),
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
				new ProhibitedFilenameChore()
		);
		ChoreContext currentContext = GitChoreContext.newBuilder(root).build();
		for (Chore chore : chores) {
			currentContext = chore.doit(currentContext);
		}
		currentContext = currentContext.refresh();
		currentContext.deleteIgnoredFiles();
		currentContext.refresh();
	}

}
