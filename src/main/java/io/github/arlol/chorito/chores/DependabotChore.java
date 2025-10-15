package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.function.Predicate;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DependabotConfigFile;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class DependabotChore implements Chore {

	DependabotConfigFile dependabotConfigFile = new DependabotConfigFile();

	@Override
	public ChoreContext doit(ChoreContext context) {
		if (context.remotes()
				.stream()
				.noneMatch(s -> s.startsWith("https://github.com"))
				&& context.textFiles()
						.stream()
						.map(MyPaths::getParent)
						.noneMatch(path -> path.endsWith(".github"))) {
			return context;
		}
		Path dependabotYml = context.resolve(".github/dependabot.yml");
		if (FilesSilent.exists(dependabotYml)) {
			dependabotConfigFile = new DependabotConfigFile(
					FilesSilent.readString(dependabotYml)
			);
		} else {
			context.setDirty();
			dependabotConfigFile = new DependabotConfigFile();
		}

		addEcosystemIfFileExists("pom.xml", "maven", context);
		addEcosystemIfFileExists("Gemfile.lock", "bundler", context);
		dependabotConfigFile.addEcosystemInDirectory("github-actions", "/");
		addEcosystemIfFileNameMatches("(?i).*dockerfile", "docker", context);
		addEcosystemIfFileExists("Pipfile", "pip", context);
		addEcosystemIfFileExists("package.json", "npm", context);
		addEcosystemIfFileExists("build.gradle", "gradle", context);
		addEcosystemIfFileExists(".terraform.lock.hcl", "terraform", context);
		addCompositeGitHubActions(context);

		dependabotConfigFile.changeDailyScheduleToMonthly();
		dependabotConfigFile.addCooldownIfMissing();

		FilesSilent.writeString(dependabotYml, dependabotConfigFile.asString());

		return context;
	}

	private void addCompositeGitHubActions(ChoreContext context) {
		addEcosystemIfFilterMatches(context, path -> {
			if (path.endsWith("action.yml") || path.endsWith("action.yaml")) {
				return FilesSilent.readString(path)
						.contains("using: composite");
			}
			return false;
		}, "github-actions");
	}

	private void addEcosystemIfFileExists(
			String fileName,
			String ecosystem,
			ChoreContext context
	) {
		addEcosystemIfFilterMatches(context, path -> {
			return path.endsWith(fileName);
		}, ecosystem);
	}

	private void addEcosystemIfFileNameMatches(
			String fileNamePattern,
			String ecosystem,
			ChoreContext context
	) {
		addEcosystemIfFilterMatches(context, path -> {
			return MyPaths.getFileNameAsString(path).matches(fileNamePattern);
		}, ecosystem);
	}

	private void addEcosystemIfFilterMatches(
			ChoreContext context,
			Predicate<? super Path> predicate,
			String ecosystem
	) {
		context.textFiles()
				.stream()
				.filter(predicate)
				.map(context::resolve)
				.map(path -> getRootRelativePath(context.root(), path))
				.distinct()
				.forEach(rootRelativePath -> {
					dependabotConfigFile.addEcosystemInDirectory(
							ecosystem,
							rootRelativePath
					);
				});
	}

	private String getRootRelativePath(Path root, Path path) {
		String rootRelativePath = root.relativize(path.getParent()).toString();
		if (!rootRelativePath.startsWith("/")) {
			rootRelativePath = "/" + rootRelativePath;
		}
		if (!rootRelativePath.endsWith("/")) {
			rootRelativePath += "/";
		}
		return rootRelativePath;
	}

}
