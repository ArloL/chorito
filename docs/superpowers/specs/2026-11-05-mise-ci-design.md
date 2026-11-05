# Move CI from setup-java to mise

## Why

Local builds install the JDK from `.tool-versions` through mise. CI installs it
through `setup-java` or `setup-graalvm`, which do not read that file the same
way. `newlinechecker` shows the result: `graalvm-community-25` locally, Oracle
GraalVM in CI, and Temurin in the jobs that build no native image. The goal is
one tool and one declared JDK for local and CI. Chorito carries the change, so
no repo is migrated by hand.

## Goals

- Every `actions/setup-java` step in a chorito-managed repo becomes
  `jdx/mise-action`, reading an exact version from `.tool-versions`.
- GraalVM repos follow the newest GraalVM Community innovation release. These
  tools do not need LTS-style versions.
- Temurin repos pin an exact Temurin version too.
- Every job in a repo uses the same JDK, including jobs that build no native
  image. Those jobs skip the native build instead of using a different JDK.
- Publishing to GitHub Packages keeps working without `setup-java`.

## Non-goals

- `mise.toml` and `mise.lock`. Exact pins already give one JDK everywhere, and
  updates by PR. Committed checksums are given up on purpose.
- Caching the JDK.
- `jitpack.yml`, which sets its own JDK.
- Repos with no Java, such as those whose `.tool-versions` holds only `nodejs`
  or `go`.
- Gluon repos, which `updateGraalSteps` already excludes.
- A `setup-java` step reading a version file other than the root
  `.tool-versions`. It stays as it is, and the sweep reports it.
- A shared GitHub Action for Maven settings. Reconsider only if a repo needs
  several JDKs in one build, which is when generated toolchains would help.

## Anti-goals

- A green build that silently stopped building native images.
- A local `mvn verify` that changes behaviour. Native stays on by default
  wherever `JAVA_HOME` is a GraalVM.
- Anything that replaces a developer's `~/.m2/settings.xml` locally.
- A chorito run that leaves a diff on a second run. `mvn-exec.sh` and
  `chorito-release.sh` fail on that.

## Decided by the owner

- Chorito applies this. `newlinechecker` is not migrated directly.
- All `setup-java` steps move, so local and CI match.
- Exact versions in `.tool-versions`, with no `mise.toml` or `mise.lock`.
- GraalVM repos use innovation releases, on the latest version. Temurin repos are
  pinned exactly too.
- Non-native jobs skip native through an explicit, long environment variable that
  tells a passerby what it does.
- Maven settings come from a committed settings file, for now.
- The migration runs locally in `~/Developer/chorito-main/projects`, not through
  the chores workflow, which cannot change workflow files.
- It is tried first against `newlinechecker`, `pkl-config-java-native` and
  `drifty`, run locally.
- Differences in CodeQL or Sonar results on a GraalVM JDK do not block this.

## Evidence so far

Run on `ArloL/newlinechecker`, branch `ci/mise`, runs `41993332771` and
`41994615458`. Only the four native build jobs moved to mise, using `mise.toml`
and an exact version. `deploy` and `release` were skipped.

| Check | Result |
|---|---|
| macOS, Linux x64, Linux arm, Windows | All passed |
| Windows native image | `cl.exe` found with no `vcvarsall` step; 6 native tests passed |
| mise step without a cached JDK | 12–28s, on 4–9 minute jobs |

Local checks with mise 2026.11.0 and Maven 3.10.0:

| Check | Result |
|---|---|
| `!env.SKIP_GRAALVM_NATIVE_IMAGE_BUILD` in profile activation | Profile on when unset, off when set |
| `MAVEN_ARGS` with `-P!graal` or `-DskipNative` | No effect through `mvnw` |
| Settings file with `${env.*}` | `help:effective-settings` resolved the `github` server |
| `java temurin-25.0.5+8.0.LTS` in `.tool-versions` | mise resolves it |

Temurin 26, `newlinechecker` (no GraalVM):

| Command | Result |
|---|---|
| `./mvnw package` | Passes, no native image built |
| `./mvnw -P graal package` | Fails: `native-image is not installed in your JAVA_HOME` |

Maven 3.10.0 on an unknown profile: `./mvnw --batch-mode -P no-such-profile help:active-profiles`
exits 0 (`BUILD SUCCESS`) with only `[WARNING] The requested profile "no-such-profile" could
not be activated because it does not exist.`

Autobuild's Maven command, from a real CodeQL log, passes `-DskipTests` and
`-Dmaven.test.skip.exec` but nothing about native.

Probe run `42007358579` on the same branch, with `.tool-versions` and mise 2026.11.1:

| Probe | Result | Control |
|---|---|---|
| mise-action reads `.tool-versions` | macOS, Windows, Linux x64 and arm installed `25.0.5.0.3` | JDK version and `lib/graal` checked |
| Exact Temurin | `25.0.5+8.0.LTS` installed | No `lib/graal` |
| Skip variable through `autobuild`, no `setup-java` files | 0 native images, job took 51s | Without it: 1 native image, 3m 53s |
| Settings file authenticates | Downloaded a published pom from GitHub Packages with `packages: read` | Without it: 401 |

Local Renovate 44.217.0 dry runs, with a 7 day minimum age:

| Setup | Result |
|---|---|
| GraalVM custom manager | `25.0.3` to `25.0.4.1.1`; the two-day-old `25.0.5.0.3` held back |
| Temurin, asdf manager | Finds `25.0.5+8.0.LTS`, but pending: `java-version` has no release dates |
| Temurin, `minimumReleaseAge: null` for `java-version` | Update no longer pending |

Renovate's asdf manager reads `.tool-versions` by default and maps
`temurin-<version>` to the `java-version` datasource. It skips GraalVM as
`unsupported-datasource`, which is why a custom manager is needed.

## Design

### Declaring the JDK

`IdiomaticVersionFileChore` keeps `.tool-versions` as the only version file, and
makes its `java` line exact. Other lines stay untouched.

| Current `java` line | Result |
|---|---|
| None, pom has `native-maven-plugin` | `java graalvm-community-innovation-<GRAALVM>` |
| None, other Java | `java temurin-<TEMURIN>` |
| `graalvm-community-innovation-*` | Unchanged |
| Other `graalvm-community-*` | `java graalvm-community-innovation-<GRAALVM>` |
| `temurin-25` | `java temurin-<TEMURIN>` |
| `temurin-<exact>`, or anything else | Unchanged |

`JavaVersions` holds the two constants: `GRAALVM` starts at `25.0.5.0.3` and
`TEMURIN` at `25.0.5+8.0.LTS`. Renovate bumps both in chorito, as it already does
for `TEMURIN`, so moving to a new JDK major is a Renovate PR or a chorito change.

The chore runs before the workflow chores, since they read the result.

### Updates

| Repo type | Mechanism |
|---|---|
| GraalVM | Custom regex manager on `.tool-versions`, with the `graalvm-community-innovation` custom datasource already used in `pkl-config-java-native` |
| Temurin | Renovate's built-in asdf manager, with `minimumReleaseAge: null` for the `java-version` datasource |

`RenovateChore` adds the custom datasource and manager to GraalVM repos, and
removes the old `java graalvm-community-(?<currentValue>\S+)` manager, whose
`jdk-*` tags do not include innovation releases. A repo that already has them,
like `pkl-config-java-native`, keeps its config, including the group tying the
JDK to its Truffle jars. No exact-pin detection is needed.

The `java-version` datasource reads Adoptium's `/v3/info/release_versions`, which
has no dates, so the minimum age would hold every Temurin update as pending. The
package rule turns the minimum age off for that datasource, so Temurin updates
skip the 7 day wait until Renovate adds dates. Chorito needs the same rule for its
own `TEMURIN` constant. The rule's comment links the Renovate discussion, so it can
be removed later.

Updates run in the existing window (`* * 20-23 * *`, 7 day minimum age), so
"latest" can lag by about a month.

### Workflows

Chorito's own workflows are the templates (`Template.load` reads
`github-settings/workflows/`), so the change starts there. Each `setup-java` step
becomes:

```yaml
- uses: jdx/mise-action@<sha> # vX
  env:
    # renovate: datasource=github-releases depName=jdx/mise extractVersion=^v(?<version>.+)
    MISE_VERSION: 2026.11.1
  with:
    cache: false
    version: ${{ env.MISE_VERSION }}
```

A step that used `cache: maven` is also followed by:

```yaml
- uses: actions/cache@<sha> # vX
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-${{ runner.arch }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: ${{ runner.os }}-${{ runner.arch }}-maven-
```

Platform jobs build with `-P graal` and have no guard step. A profile activated
automatically can switch itself off, as on a JDK without `lib/graal`, and the build
stays green with no native image. `-P graal` forces the profile, so a JDK without
`native-image` fails the job instead (see Evidence). Unix jobs have `-P graal \` after
`--batch-mode \`; Windows has `.\mvnw --batch-mode -P graal ...`.

After migration a workflow no longer mentions `graalvm`, so `updateGraalSteps` and
`NativeReleaseLayoutChore` must detect GraalVM from `.tool-versions`.
`JavaVersions.buildsOnGraalVm` stays for this. `pinsGraalVmInnovation` goes, and
innovation repos now get the same platform jobs as any other.

### Skipping native

Every profile containing `native-maven-plugin` gets exactly this activation, whatever it
had before, including none. Maven requires both conditions to hold:

```xml
<activation>
  <file>
    <exists>${env.JAVA_HOME}/lib/graal</exists>
  </file>
  <property>
    <name>!env.SKIP_GRAALVM_NATIVE_IMAGE_BUILD</name>
  </property>
</activation>
```

When a pom has exactly one profile containing the plugin, its `<id>` is also set to
`graal`, because `-P graal` selects by id and Maven only warns about an unknown one
(see Evidence), which would build no native image with a green result. With two or more,
ids are left alone, since duplicates would be invalid.

This covers every pom, because `pkl-config-java-native` has the profile in a child
module.

In GraalVM repos, each step outside the platform jobs that runs `mvnw` or CodeQL's
`autobuild` sets `SKIP_GRAALVM_NATIVE_IMAGE_BUILD: true`, and Sonar's
`-P '!graal'` goes away. Other repos have no such profile, and do not get the
variable.

### Maven settings

Repos whose `deploy` job uses `altDeploymentRepository` get
`.github/maven-settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>${env.GITHUB_ACTOR}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
```

Only the deploy step passes `--settings .github/maven-settings.xml`. Do not use
`.mvn/maven.config`, because that also applies locally. No toolchains file: no
pom or workflow uses one.

### Code to remove

- `JavaVersions.pinsGraalVmInnovation`.
- `GitHubActionsWorkflowFile`: `useToolVersionsFileForSetupJava`,
  `getPinnedJavaVersion`, `pinTemurinJavaVersion`, `useToolVersionsFile`, and the
  Temurin helpers.
- `GitHubActionChore.useToolVersionsForSetupJava`.
- `CodeQlAnalysisChore`: the Temurin pin. Its `removeActionFromJob` for non-Java
  repos must target the new steps.
- `RenovateChore`: the old GraalVM manager in `GRAAL_MIGRATIONS`.
- `Template.generalise`: the `useToolVersionsFile` call.

## Prove before release

The probes above settled how mise, the skip variable, the settings file and
Renovate behave. What remains is running the real chore output.

Run chorito from source against `newlinechecker`, `pkl-config-java-native` and
`drifty`, from this branch's checkout. Open a PR from each. Between them they
cover a native CLI, a repo with Truffle jars and a child-module profile, and the
repo where CI conventions start.

`reset_to_main` hard-resets each repo and deletes its local branches, so commit
and push each result before running it again.

Still open:

1. A real upload: `deploy` only runs on pushes to `main`, and the probe only read
   from GitHub Packages. Merge `newlinechecker` first, and check this before
   merging anything else.
2. CodeQL's `analyze` on a GraalVM JDK. The owner accepts differences in results,
   but the job has to pass.

## Rollout

1. Try it on the three repos above, from source, without merging chorito.
2. Merge those three repos' PRs, `newlinechecker` first.
3. Run the Task 8 loop from the chorito worktree for every repo in `mvn-exec.sh`'s
   list except `chorito` (`mvn-exec.sh` itself `cd`s to `ws/chorito`, which is
   `main`), and open a PR for each changed repo, with rebase auto-merge as usual
   for chore PRs. Chorito is excluded: this branch on chorito `main` changes 7
   files and the templates are symlinks into `.github`, so an auto-merged PR would
   release a binary with new templates and old chores and dispatch it to 10
   repos. Chorito changes only through this branch.
4. Merge chorito once those are in. Every merge to its `main` releases and
   dispatches chores to the 10 repos in `trigger-chores`, and a chores run that
   needs to change a workflow cannot push it. Until chorito merges, the released
   chorito changes `codeql-analysis.yaml` back to `setup-java`, CI cannot push
   that, and the Chores run fails with no PR opened. Nothing bad reaches `main`.
   Monthly crons and any merge to chorito's `main` trigger this. Keep the window
   short, hold other chorito merges during it, do not run `chorito-release.sh`
   during it, and expect some red Chores runs.
5. Update `~/Developer/chorito-main/projects/AGENTS.md`, whose `setup-java` note
   becomes wrong. Delete the `ci/mise` branch from `ArloL/newlinechecker`.

## Scope at time of writing

From local checkouts, some behind `main`: 18 repos with `setup-java`, 95 steps
(24 GraalVM, 71 Temurin). The chore must not assume these counts.

`.tool-versions` `java` lines: 11 `temurin-25`, 3 `graalvm-community-25.0.2`,
3 `graalvm-community-25`, 1 `graalvm-community-innovation-25.0.4.1.1`.

GraalVM repos: `chorito`, `drifty`, `git-dora-lead-time-calculator`,
`newlinechecker`, `pkl-config-java-native`, `template-graal`, `wait-for-ports`.

## Calls made without the owner

- `SKIP_GRAALVM_NATIVE_IMAGE_BUILD` as the name. The owner asked for explicit and
  long, not this exact name.
- `cache: false` for the JDK, keeping the `~/.m2` cache.
- Pinning the mise version, with Renovate bumping it.
- Starting versions `25.0.5.0.3` and `25.0.5+8.0.LTS`.
- Leaving a `temurin-<other major>` selector as it is.
- Setting the skip variable on every non-platform job, not a named list of steps.
- Merging repo PRs before chorito, and `newlinechecker` first.
- Overwriting a deliberately different activation on a `native-maven-plugin` profile.
- Renaming a lone `native-maven-plugin` profile to `graal`.
- Proving `deploy` on a real merge rather than a temporary branch.

## Left to the builder

- The order of commits, and how to keep each one green.
- How to edit the pom's profile activation through `MavenPomFile`.
- Which pieces of old fixtures to delete or rewrite.
