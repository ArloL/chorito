package io.github.arlol.chorito.commands;

import java.util.List;

import io.github.arlol.chorito.chores.Chore;
import io.github.arlol.chorito.chores.CodeFormatterProfileChore;
import io.github.arlol.chorito.chores.CodeQlAnalysisChore;
import io.github.arlol.chorito.chores.DeleteUnnecessaryFilesChore;
import io.github.arlol.chorito.chores.DeleteUnwantedFilesChore;
import io.github.arlol.chorito.chores.DependabotChore;
import io.github.arlol.chorito.chores.DockerfileChore;
import io.github.arlol.chorito.chores.Ec4jChore;
import io.github.arlol.chorito.chores.EclipseFormatterPluginChore;
import io.github.arlol.chorito.chores.EditorConfigChore;
import io.github.arlol.chorito.chores.GitAttributesChore;
import io.github.arlol.chorito.chores.GitHubActionChore;
import io.github.arlol.chorito.chores.GitIgnoreChore;
import io.github.arlol.chorito.chores.GitMasterBranchChore;
import io.github.arlol.chorito.chores.GraalNativeImageMavenPluginMigrationChore;
import io.github.arlol.chorito.chores.GraalNativeImagePropertiesChore;
import io.github.arlol.chorito.chores.GradleWrapperChore;
import io.github.arlol.chorito.chores.IntellijChore;
import io.github.arlol.chorito.chores.JavaUpdaterChore;
import io.github.arlol.chorito.chores.JitpackChore;
import io.github.arlol.chorito.chores.LicenseChore;
import io.github.arlol.chorito.chores.LifecycleMappingChore;
import io.github.arlol.chorito.chores.MavenWrapperChore;
import io.github.arlol.chorito.chores.ModernizerPluginChore;
import io.github.arlol.chorito.chores.PomParentRelativePathChore;
import io.github.arlol.chorito.chores.PomPropertiesChore;
import io.github.arlol.chorito.chores.PomScmChore;
import io.github.arlol.chorito.chores.ProhibitedFilenameChore;
import io.github.arlol.chorito.chores.ReadmeChore;
import io.github.arlol.chorito.chores.RemoveUnnecessaryExecFlagsChore;
import io.github.arlol.chorito.chores.SpotbugsPluginChore;
import io.github.arlol.chorito.chores.VsCodeChore;
import io.github.arlol.chorito.chores.XmlPreambleChore;
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
