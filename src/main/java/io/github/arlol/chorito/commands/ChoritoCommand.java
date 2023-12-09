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
import io.github.arlol.chorito.chores.JavaUpdaterChore;
import io.github.arlol.chorito.chores.JitpackChore;
import io.github.arlol.chorito.chores.LicenseChore;
import io.github.arlol.chorito.chores.MavenWrapperChore;
import io.github.arlol.chorito.chores.PomParentRelativePathChore;
import io.github.arlol.chorito.chores.PomPropertiesChore;
import io.github.arlol.chorito.chores.PomScmChore;
import io.github.arlol.chorito.chores.ProhibitedFilenameChore;
import io.github.arlol.chorito.chores.ReadmeChore;
import io.github.arlol.chorito.chores.RemoveUnnecessaryExecFlagsChore;
import io.github.arlol.chorito.chores.SpotbugsPluginChore;
import io.github.arlol.chorito.chores.XmlPreambleChore;
import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.GitChoreContext;

public class ChoritoCommand {

	private final ChoreContext context;

	public ChoritoCommand(ChoreContext context) {
		this.context = context;
	}

	public ChoritoCommand(String root) {
		this.context = GitChoreContext.newBuilder(root).build();
	}

	public void execute() {
		List<Chore> chores = List.of(
				new GitMasterBranchChore(context),
				new ReadmeChore(context),
				new GitAttributesChore(context),
				new EditorConfigChore(context),
				new PomParentRelativePathChore(context)
		);
		chores.forEach(Chore::doit);

		new PomScmChore(context).doit();
		new PomPropertiesChore(context).doit();
		new LicenseChore(context).doit();
		new XmlPreambleChore(context).doit();
		new MavenWrapperChore(context).doit();
		new GradleWrapperChore(context).doit();
		new DockerfileChore(context).doit();
		new DependabotChore(context).doit();
		new CodeQlAnalysisChore(context).doit();
		new GitHubActionChore(context).doit();
		new GitIgnoreChore(context).doit();
		new JavaUpdaterChore(context).doit();
		new JitpackChore(context).doit();
		new GraalNativeImagePropertiesChore(context).doit();
		new Ec4jChore(context).doit();
		new DeleteUnnecessaryFilesChore(context).doit();
		new DeleteUnwantedFilesChore(context).doit();
		new ProhibitedFilenameChore(context).doit();
		new GraalNativeImageMavenPluginMigrationChore(context).doit();
		new RemoveUnnecessaryExecFlagsChore(context).doit();
		new CodeFormatterProfileChore(context).doit();
		new EclipseFormatterPluginChore(context).doit();
		new SpotbugsPluginChore(context).doit();
	}

}
