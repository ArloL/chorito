package io.github.arlol.chorito.chores;

import java.nio.file.Path;
import java.util.function.Predicate;

import io.github.arlol.chorito.tools.ChoreContext;
import io.github.arlol.chorito.tools.DependabotConfigFile;
import io.github.arlol.chorito.tools.FilesSilent;
import io.github.arlol.chorito.tools.MyPaths;

public class DependabotChore implements Chore {

	DependabotConfigFile dependabotConfigFile;
	ChoreContext context;

	@Override
	public ChoreContext doit(ChoreContext context) {
		this.context = context;
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

		addEcosystemIfFileExists("pom.xml", "maven");
		addEcosystemIfFileExists("Gemfile.lock", "bundler");
		dependabotConfigFile.addEcosystemInDirectory("github-actions", "/");
		addEcosystemIfFileNameMatches("(?i).*dockerfile", "docker");
		addEcosystemIfFileExists("Pipfile", "pip");
		addEcosystemIfFileExists("package.json", "npm");
		addEcosystemIfFileExists("build.gradle", "gradle");
		addEcosystemIfFileExists(".terraform.lock.hcl", "terraform");
		addCompositeGitHubActions();

		FilesSilent.writeString(dependabotYml, dependabotConfigFile.asString());

		return context;
	}

	private void addCompositeGitHubActions() {
		addEcosystemIfFilterMatches(path -> {
			if (path.endsWith("action.yml") || path.endsWith("action.yaml")) {
				return FilesSilent.readString(path)
						.contains("using: composite");
			}
			return false;
		}, "github-actions");
	}

	private void addEcosystemIfFileExists(String fileName, String ecosystem) {
		addEcosystemIfFilterMatches(path -> {
			return path.endsWith(fileName);
		}, ecosystem);
	}

	private void addEcosystemIfFileNameMatches(
			String fileNamePattern,
			String ecosystem
	) {
		addEcosystemIfFilterMatches(path -> {
			return MyPaths.getFileName(path)
					.toString()
					.matches(fileNamePattern);
		}, ecosystem);
	}

	private void addEcosystemIfFilterMatches(
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
