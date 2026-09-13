# Modularity Review

**Scope**: chorito — entire codebase (`src/main/java`, 76 classes across `chores`, `tools`, `filter`, `commands`, plus the patched `org.ec4j.lint.api` class and the `src/main/resources` template symlinks)
**Date**: 2026-09-13

## Executive Summary

chorito is a single-binary CLI that walks a target repository and applies about forty `Chore` implementations in a fixed order, rewriting configuration files — GitHub Actions workflows, Maven poms, `.gitignore`, editor settings, `renovate.json5` — in place. Its [modularity](https://coupling.dev/posts/core-concepts/modularity/) is largely healthy: the `Chore` interface is a genuine [integration contract](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/), `ChoreContext` cleanly encapsulates filesystem and git access behind an explicit surface, and the YAML and JSON formats each have a purpose-built wrapper that keeps parser types out of chore code. The problems are not in the class structure; they are in the knowledge that travels *around* it — through the filesystem, through symlinks, and through regular expressions in build configuration.

The most important finding is that chorito's own repository configuration **is** the payload it ships. `src/main/resources/io/github/arlol/chorito/github-settings` is a symlink to chorito's own `.github`, and the same holds for the Eclipse, VS Code and IntelliJ template directories. Every edit to chorito's own CI is simultaneously an edit to what every managed repository receives, and the code already carries two hand-written workarounds for exactly this. The second finding follows from it: three chores independently rewrite `.github/workflows/*`, sharing an unwritten agreement about who owns which job, which permission, and which schedule field.

A note on the stated pain point — stacking generations of migrations and testing them. The [balance rule](https://coupling.dev/posts/core-concepts/balance/) says the co-location of those migrations is *correct*, not a mistake. What makes them expensive is that their ordering is implicit and their set only grows. That is a different problem with a different fix, and it is addressed as Issue 3 rather than as a decomposition recommendation.

## Coupling Overview

| Integration | [Strength](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | [Distance](https://coupling.dev/posts/dimensions-of-coupling/distance/) | [Volatility](https://coupling.dev/posts/dimensions-of-coupling/volatility/) | [Balanced?](https://coupling.dev/posts/core-concepts/balance/) |
| --- | --- | --- | --- | --- |
| chorito's own `.github`/`.settings` → shipped templates (symlink) | [Functional](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/), implicit | High — one file, two lifecycles | High — core, constantly edited | **No — critical** |
| `GitHubActionChore` ↔ `AttestReleaseAssetsChore` ↔ `CodeQlAnalysisChore` (via the filesystem) | [Functional](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/), implicit | Medium–high — no compiler carries the contract | High — core | **No — critical** |
| Migration *N* → migration *N+1* inside `GitHubActionChore` | [Functional](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/), implicit | Low — same class, same method | High — core | Balanced by the rule, but implicit and unbounded |
| `renovate.json5` → `SpotbugsPluginChore`, `ModernizerPluginChore`, `JavaVersions` source text | [Intrusive](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | High — separate tool, separate schedule, no compiler | Medium — renames and reformatting | **No — significant** |
| `CustomLinterRegistryBuilder` → ec4j package-private internals | [Intrusive](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | Highest in the system — different organisation | Low functional, medium implementation (Renovate bumps it) | **No — significant, fragile** |
| 14 chores → jsoup `Document`/`Element` + duplicated selector strings | [Model](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) + functional | High — third-party library | Low functional, demonstrated implementation | **No — significant** |
| 31 file-writing chores → `ChoreContext.setDirty()` protocol | [Functional](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/), implicit | Medium — across packages | Medium — grows with each new chore | **No — minor** |
| `EclipseFormatterPluginChore` → `Spotbugs` → `Modernizer` → `MavenJavadocSources` | [Functional](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/), implicit | Medium | Low — supporting subdomain, stable conventions | Accepted — neutralised by low volatility |
| Chores → `ChoreContext` (`root`, `textFiles`, `remotes`, `clock`, `randomGenerator`) | [Contract](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | Medium | Medium | **Yes — healthy** |
| `ChoritoCommand` → `Chore` interface | [Contract](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) | Low–medium | Medium | **Yes — healthy** |
| Chores → `GitHubActionsWorkflowFile`, `DependabotConfigFile`, `JsonBuilder` | [Contract](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) (leaking `Node` in places) | Medium | High | **Yes — healthy, worth completing** |

---

## Issue 1: Shipped templates are chorito's own repository configuration

**Integration**: chorito's own `.github`, `.settings`, `.vscode`, `.idea` → the template resources every managed repository receives
**Severity**: Critical

### Knowledge Leakage

`src/main/resources/io/github/arlol/chorito/github-settings` is a symlink to `../../../../../../../.github`. The same pattern holds for `eclipse-settings → .settings`, `vscode-settings → .vscode` and `idea-settings → .idea`. One set of files therefore serves two entirely different requirement sets: what *chorito* needs to build and release itself, and what *an arbitrary managed repository* should be given as a default.

These two requirement sets are not the same, and the code says so out loud in two places. `GitHubActionChore.REQUIRED_PERMISSIONS` carries this Javadoc:

> Read from here rather than off the template, because the template is chorito's own workflow: chorito attests its releases and so grants itself `attestations` and `id-token`, and copying that across would hand those to every release job whether or not it publishes anything.

And `CodeQlAnalysisChore`:

> Both branches are spelled out because the template is a symlink to the workflow chorito runs on itself: whatever chorito's own JDK setup happens to say must not decide what every other repository gets.

Each of those comments documents a place where a maintainer noticed the leak and wrote a manual correction. The [shared knowledge](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) — "which parts of my own CI are universal and which are mine alone" — exists nowhere as data. It lives in prose, scattered across the chores that happened to trip over it.

### Complexity Impact

This is [complexity](https://coupling.dev/posts/core-concepts/complexity/) in the Cynefin sense: the outcome of editing `.github/workflows/main.yaml` can only be determined by making the change and observing what every managed repository ends up with. Nothing in the Java code references the symlink. A maintainer editing chorito's own release workflow to add a step, grant a permission, or pin a toolchain is editing the product's output without any signal that they are doing so. To predict the effect they must hold in working memory: the edit itself, the fact that the resource is a symlink, which chores read that template, which of those chores apply corrections, and what those corrections are. That is comfortably past the 4±1 units the model budgets for.

The symlink is also invisible from the direction a reader approaches it. `ClassPathFiles.readString("github-settings/workflows/main.yaml")` looks like an ordinary resource read. Nothing at that call site suggests the resource is live, shared, and under active edit for a different purpose.

### Cascading Changes

- **chorito adopts a CI capability for itself.** Release attestation is the worked example: adding `id-token` and `attestations` to chorito's own release job silently proposed those permissions for every managed repository's release job. The correction was a new constant, a new Javadoc paragraph, and an entire new chore (`AttestReleaseAssetsChore`) to grant them only where earned.
- **chorito changes its own JDK.** chorito builds a native image and so pins GraalVM in `.tool-versions`. Because the CodeQL template is chorito's own workflow, the analysis job would inherit a GraalVM it never uses. The correction was to spell out both branches explicitly in `CodeQlAnalysisChore` and add `JavaVersions.buildsOnGraalVm` to distinguish them.
- **chorito changes an editor setting.** A personal Eclipse formatter or VS Code preference in `.settings`/`.vscode` ships to every managed repository on the next release, with no review step that distinguishes "my preference" from "our standard".

The cost of each cascade is bounded — one maintainer, one deployable — but it is paid *every time*, and the corrections accumulate as special cases spread across chores rather than as one declared difference.

### Recommended Improvement

Do not break the symlink. The dogfooding is genuinely valuable: it guarantees the template is a workflow that actually runs, and that guarantee is worth keeping.

Instead, reduce [integration strength](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) by making the difference between "chorito's own" and "everyone's" **explicit and declared in one place** — an [anti-corruption layer](https://coupling.dev/posts/related-topics/domain-driven-design/) over the template. Concretely: introduce a `Template` type in `tools` that is the only thing permitted to read `github-settings/**`, and give it a declared list of chorito-specific elements to strip or neutralise on load — the `attestations`/`id-token` permissions, the GraalVM toolchain pin, chorito-only steps. Chores then consume `Template.mainWorkflow()` rather than `ClassPathFiles.readString(...)`, and get a payload that is already general.

The trade-off: one more indirection, and a list that must be maintained as chorito's own CI evolves. That is worth it because the list *is* the knowledge currently spread across two Javadoc comments and two hand-rolled branches — making it explicit converts [implicit functional coupling into contract coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/), which is the single highest-leverage move available here. A cheaper first step that captures much of the value: a test asserting that the loaded template grants no `attestations`, `id-token` or GraalVM setup, so the next leak fails the build instead of shipping.

---

## Issue 2: Three chores co-own the workflow files with no contract between them

**Integration**: `GitHubActionChore` ↔ `AttestReleaseAssetsChore` ↔ `CodeQlAnalysisChore`, communicating through `.github/workflows/*.yaml`
**Severity**: Critical

### Knowledge Leakage

Three chores rewrite the same files, and they share a substantial body of knowledge that is never stated as a contract:

- **Job names.** `version`, `release`, `debug` and `analyze` appear as string constants in more than one chore. Each is a piece of domain vocabulary about what a chorito-managed workflow looks like.
- **Permission ownership.** `GitHubActionChore.REQUIRED_PERMISSIONS` grants `contents: write` and `packages: write`; `AttestReleaseAssetsChore.ATTEST_PERMISSIONS` grants `id-token: write` and `attestations: write`. Both call `grantJobPermissions("release", …)` on the same job of the same file. The rule partitioning them exists only in a Javadoc comment on one of the two constants.
- **File ownership.** `CodeQlAnalysisChore` fully regenerates `.github/workflows/codeql-analysis.yaml` from the template. `GitHubActionChore.updateCodeQlSchedule` then re-reads that same file and may rewrite its cron expression.

The last one is the sharpest example, because it currently works only by accident. `updateCodeQlSchedule` rewrites the cron when `!currentCron.endsWith("*")`. `CodeQlAnalysisChore` writes a cron from `RandomCronBuilder.randomDayOfMonth()`, which produces `minute hour dayOfMonth * *` — it always ends in `*`, so the second write never fires. The invariant holding the two chores apart is an implementation detail of a *third* component, `RandomCronBuilder`, and it is written down nowhere. Calling `.month()` or `.dayOfWeek()` in that builder — a change with no visible connection to either chore — would make the two chores start overwriting each other's schedules.

### Complexity Impact

The [distance](https://coupling.dev/posts/dimensions-of-coupling/distance/) here is larger than the package layout suggests. These classes sit side by side in `chores/`, but they do not call each other: they communicate by writing bytes to a file that the next one reads. No compiler, type system, or signature carries the agreement. Effective distance is therefore that of an untyped, asynchronous channel, while [integration strength](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) is functional — they share business rules about what a correct workflow contains. High strength at meaningful distance in a high-[volatility](https://coupling.dev/posts/dimensions-of-coupling/volatility/) area is precisely the unbalanced quadrant the model warns about.

Adding a fourth workflow-touching chore requires knowing, for every field it wants to write, whether one of the existing three already owns it — and answering that means reading 844 lines of `GitHubActionChore` plus two more classes. The effect of a change can only be established by running the whole pipeline and diffing.

### Cascading Changes

- **Adding a permission to a job.** The maintainer must first determine which of the two permission maps owns that job, a fact recorded only in prose.
- **Renaming a job.** The string `"release"` is a constant in two files and `"version"` in two more. A rename that misses one produces a workflow that is silently only half-updated — no error, just an absent change.
- **Changing `RandomCronBuilder`.** As above: adding a randomised month or day-of-week field activates a dormant conflict between `CodeQlAnalysisChore` and `GitHubActionChore` with no textual link between cause and effect.
- **Reordering `ChoritoCommand`'s list.** The list is the only place the execution order is recorded, and correctness depends on it. `CodeQlAnalysisChore` at index 14 must precede `GitHubActionChore` at 15, which must precede `AttestReleaseAssetsChore` at 16.

### Recommended Improvement

Make the workflow file a single owned object rather than a shared mutable file. Give `GitHubActionsWorkflowFile` — which already exists and already encapsulates the YAML — responsibility for being read once, passed to each interested chore, and written once. Concretely: introduce a narrow interface (`WorkflowChore`, with a `void apply(GitHubActionsWorkflowFile workflow, ChoreContext context)`) and have the driver load each workflow, run the interested chores against the in-memory object, then write it back if it changed. Distance drops from "untyped file channel" to "typed method parameter", which balances the functional strength that genuinely exists between them.

Alongside that, lift the shared vocabulary into one place: a `WorkflowJobs` constants holder for `version`/`release`/`debug`/`analyze`, and a single permissions map keyed by job with a comment explaining the attestation carve-out, so the partition is data rather than prose.

The trade-off is a second interface next to `Chore` and a driver that treats workflow chores specially. That asymmetry is justified: workflows are chorito's [core subdomain](https://coupling.dev/posts/dimensions-of-coupling/volatility/) — the highest-volatility part of the system, and the part where the file-as-channel pattern costs the most. The other thirty-odd chores touch files nobody else touches and should keep the simple `Chore` contract.

---

## Issue 3: Migration order is an implicit contract, and the set only grows

**Integration**: migration *N* → migration *N+1* within `GitHubActionChore.doit()`
**Severity**: Significant

### Knowledge Leakage

`GitHubActionChore.doit()` calls 27 private migration methods in sequence. Several consume text that an earlier one produced, and nothing marks the dependency:

- `useSpecificActionVersions` rewrites `actions/setup-java@v3` to `@v3.5.1` (lines 393–394). `migrateToGraalSetupAction` matches a literal block containing `actions/setup-java@v3.5.1` (lines 327, 350). The second only ever matches because the first ran.
- `migrateToGraalSetupAction`'s target block contains `distribution: adopt` (lines 330, 353). `migrateJavaDistributionFromAdoptToTemurin` destroys that exact string (line 309). Swap the two calls and the Graal migration silently stops matching — no error, just a repository that never gets migrated.
- `migrateActionsCreateRelease` emits an `ncipollo/release-action@v1.13.0` block (line 186) and `migrateEregonPublishRelease` emits another at a pinned SHA (line 617). `migrateNcipoploReleaseAction` normalises any ncipollo version and rewrites the surrounding block (line 639), consuming both.

The knowledge being shared is each migration's *exact output text*, and the only record of the ordering it implies is the sequence of calls in `doit()`.

### Complexity Impact

Worth stating plainly, because it cuts against the intuition that a 27-step chain must be a design flaw: **by the [balance rule](https://coupling.dev/posts/core-concepts/balance/), this coupling is balanced.** Integration strength is high (functional — the migrations share knowledge of each other's output), but distance is the lowest available: same class, same method, same file, same commit, same maintainer. `STRENGTH XOR DISTANCE` holds. This is [high cohesion](https://coupling.dev/posts/core-concepts/balance/), and splitting the chained migrations into separate classes or services would *raise* distance against unchanged strength and make things strictly worse. The instinct to decompose should be resisted here.

What makes the area expensive is two other things:

**The ordering is implicit.** A reader cannot tell, from any migration, which others it depends on. The knowledge is real and correctly co-located; it is simply not written down.

**The set is unbounded and untestable in pieces.** Of 29 tests in `GitHubActionChoreTest`, 26 invoke the full `doit()`; only `updateGraalSteps` and `updatePermissions` can be exercised alone, and they are `public` solely to permit that. Every test of every migration therefore runs all 27 against its fixture. Adding migration 28 means its output must be checked against 27 predecessors, and every existing fixture now passes through one more transformation. This is the stated pain, and it is a cohesion-of-testing problem rather than a coupling imbalance.

### Cascading Changes

- **Adding a migration.** It must be positioned relative to 27 others whose input and output shapes are only discoverable by reading them, and it perturbs every existing test fixture.
- **Reordering or removing a migration.** Removing `useSpecificActionVersions` — apparently obsolete, since it pins action versions from 2022 — would silently disable `migrateToGraalSetupAction`, which depends on its output. There is nothing to warn about this.
- **Retiring a generation.** Since all managed repositories are known and kept current, migrations *can* in principle be deleted once every repository is past them. But nothing records which generation a repository has reached, so deleting one is currently a guess, and the safe move is always to keep it. The set grows monotonically.

### Recommended Improvement

Do not decompose. Make the two implicit things explicit instead.

**Name the ordering.** Replace the flat call list with an ordered, named sequence — a `List<Migration>` of small objects or method references each carrying a name — and, where a dependency exists, state it in a one-line comment at the declaration (`// must precede migrateJavaDistributionFromAdoptToTemurin: consumes "distribution: adopt"`). Three such comments would capture every chain identified above. This costs almost nothing and converts undocumented [functional coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) into a declared one.

**Make retirement decidable.** Because the managed set is closed and kept current, add the one fact that is missing: a record of what a repository has already been migrated through. A `chorito` stamp — a version or date in a small marker file, or a comment line in the generated workflow — lets a migration declare "applies to repositories last processed before X" and lets the maintainer prove a migration is dead before deleting it. Once migrations are retirable, the growth stops being monotonic and the testing burden stops compounding.

**Test migrations in isolation.** Once migrations are named objects, a test can invoke one directly against a small fixture instead of running the whole chain. Keep a handful of end-to-end tests for the chains that genuinely interact; make the rest unit tests.

The trade-off is a marker file in managed repositories and a modest refactor of one class. Against a monotonically growing, all-or-nothing test suite in the highest-volatility part of the system, that is a favourable trade.

---

## Issue 4: `renovate.json5` reads chorito's Java source as data

**Integration**: `renovate.json5` custom managers → `SpotbugsPluginChore.java`, `ModernizerPluginChore.java`, `tools/JavaVersions.java`, and two test resources
**Severity**: Significant

### Knowledge Leakage

Three custom managers in `renovate.json5` name Java source files by path and match against their contents by regular expression:

- `SpotbugsPluginChore.java` and `spotbugs-plugin/expected.xml`, matched with `<groupId>com\.github\.spotbugs</groupId>\s*<artifactId>spotbugs-maven-plugin</artifactId>\s*<version>(?<currentValue>.*?)</version>`
- `ModernizerPluginChore.java` and `modernizer-plugin/expected.xml`, similarly
- `JavaVersions.java` and `codeql/graal-expected.yaml`, matched with `TEMURIN = "(?<currentValue>.*?)"`

This is [intrusive coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/): an external tool reaches past every public interface and reads a string literal buried inside a method body — in the Spotbugs and Modernizer cases, XML assembled by Java string concatenation across a dozen source lines. The shared knowledge is not just "there is a version here" but the precise source-level spelling: the file path, the constant's name, the whitespace between XML tags, and the fact that the concatenation happens to leave them adjacent.

### Complexity Impact

The failure mode is silence. If a chore class is renamed, the XML block is reformatted, or the version is extracted into a constant, Renovate does not error — it simply stops finding the dependency, and the plugin version chorito writes into every managed repository quietly goes stale. Nothing in the build, the tests, or the type system notices. The gap between cause (a routine refactor) and effect (managed repositories drifting onto an old Spotbugs) can be months, which is the hallmark of an unpredictable change outcome.

[Distance](https://coupling.dev/posts/dimensions-of-coupling/distance/) is high: Renovate is a separate system, running on its own monthly schedule, reading a config file that no compiler checks against the source it describes.

### Cascading Changes

- **Renaming or moving a chore class** — a refactor with no apparent connection to dependency management — breaks the manager.
- **Reformatting the embedded XML**, including running a formatter over the file, can break the `\s*` matches.
- **Extracting a version to a constant** for readability breaks the match while appearing to improve the code.
- Each of these requires a matching edit to `renovate.json5`, and there is nothing to prompt it.

### Recommended Improvement

Reduce strength from intrusive to [contract](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) by creating a surface that exists *for* Renovate.

chorito has already done this once and it works: `JavaVersions.TEMURIN` is a named constant with a Javadoc that says "Renovate keeps it current through the custom manager in chorito's own `renovate.json5`." The constant is a deliberate, stable, documented integration point, and the comment tells the next reader not to inline it. Apply the same treatment to the plugin versions: a `MavenPluginVersions` holder with `SPOTBUGS`, `MODERNIZER`, `FORMATTER` and `MAVEN_SOURCE` constants, each with the same kind of Javadoc, and have the chores interpolate them into their XML. `renovate.json5` then matches one file with one simple pattern per constant, instead of four files with brittle multi-line patterns.

As a safety net, add a test asserting that the version chorito writes into a pom matches the constant — so a broken integration surfaces as a failing test rather than as silent drift.

The trade-off is one additional class and slightly less literal XML in the chores. Given that the current arrangement fails silently and the fix is already proven inside this codebase, the trade is clearly worthwhile.

---

## Issue 5: `CustomLinterRegistryBuilder` reaches into ec4j's internals

**Integration**: `src/main/java/org/ec4j/lint/api/CustomLinterRegistryBuilder` → the ec4j editorconfig-linters library
**Severity**: Significant

### Knowledge Leakage

chorito ships a class declared in the package `org.ec4j.lint.api` — a package belonging to a third-party library — inside its own source tree. The package declaration is the mechanism: it exists so the class can reach package-private members of `LinterRegistry.Builder`, `LinterEntry`, `PathSet` and `PathSet.Builder`. It subclasses them, calls their non-public constructors (`super(includes, excludes)`, `super(linter)`, `new LinterEntry(linter, pathSetBuilder.build())`), and overrides `build()` methods to substitute its own path-matching behaviour.

This is [intrusive coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) in its textbook form. The model's definition applies exactly: integration through private interfaces, where **all** knowledge of the component's implementation must be assumed shared, and where the authors of the intruded component do not know the integration exists. ec4j's maintainers made those members package-private precisely to reserve the right to change them.

### Complexity Impact

[Distance](https://coupling.dev/posts/dimensions-of-coupling/distance/) here is the highest anywhere in this system — higher than any package or module boundary, because the other party is a different organisation on an independent release cycle with no coordination channel at all. Combined with intrusive strength, this is the unbalanced quadrant at its extreme.

What saves it is [volatility](https://coupling.dev/posts/dimensions-of-coupling/volatility/) — but only partly. Editorconfig linting is a [generic subdomain](https://coupling.dev/posts/dimensions-of-coupling/volatility/): a solved problem whose *functional* requirements barely move. The model says low volatility neutralises unbalanced coupling, and that is largely true here. The qualification is that chorito's own `renovate.json5` extends `config:recommended` with a seven-day minimum release age, so ec4j (pinned at `2.2.2` via the `editorconfig-linters-bom`) is bumped automatically and on a schedule. The project has deliberately raised the *implementation* volatility of every dependency, including this one.

The likely failure is a compile error on a Renovate PR, which is loud and cheap. The unlikely but real one is a signature that still compiles while its semantics have shifted — a `contains(Path)` whose contract changed, say — which would produce wrong linting with no error at all.

### Cascading Changes

- **An ec4j minor or patch release** that alters any of the four intruded types forces a rewrite of this class, on Renovate's schedule rather than the maintainer's.
- **An ec4j release that seals or relocates the package** (a module-info, a shading change) breaks the class outright.
- **A behavioural change with a stable signature** passes the build and degrades linting silently.

### Recommended Improvement

Accept the coupling, but contain it and make failure loud. Decomposition is not on the table, and reducing strength is not really possible — the class exists because ec4j offers no public extension point for the behaviour chorito needs.

Two concrete steps. First, add a focused test that exercises `CustomLinterRegistryBuilder` through `Ec4jChore` against a fixture with a known `.editorconfig` violation and asserts the corrected output, so a semantic change in ec4j fails the build rather than silently altering behaviour. Second, treat ec4j's version as a decision rather than an automatic bump: give it a `renovate.json5` package rule that labels or groups its updates distinctly, so the PR arrives marked as one needing a look at this class rather than as routine.

Longer term, the honest fix is upstream: ask ec4j for a public extension point for path resolution. That converts intrusive coupling to [contract coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) at the only place it can be converted. Until then, the containment above is proportionate — the model explicitly permits tolerating unbalanced coupling where volatility is low, and this is that case, with a guard rail for the part of the volatility that is self-inflicted.

---

## Issue 6: XML is the one format without a wrapper, so jsoup leaks into fourteen chores

**Integration**: 14 chores → `org.jsoup.nodes.Document`/`Element`/`Node`/`Elements`, plus duplicated selector strings
**Severity**: Significant

### Knowledge Leakage

chorito handles three configuration formats and has treated them inconsistently:

- **YAML** is encapsulated. `Yamls` and `GitHubActionsWorkflowFile` wrap snakeyaml, and chores mostly work in domain terms (`hasJob`, `grantJobPermissions`, `insertStepBefore`).
- **JSON** is encapsulated. `Jsons`, `JsonBuilder`, `JsonComments` and `JsonMigrations` wrap Jackson; only `VsCodeChore` still imports an `ObjectNode`.
- **XML is not.** `JsoupSilent` wraps exactly one method — `parse` — and hands back a raw jsoup `Document`. Fourteen chores import jsoup types directly and manipulate the tree themselves.

Two kinds of knowledge leak as a result. The first is [model coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/): jsoup's document object model is shared across the chore layer. The second is worse — the *rule for locating a Maven plugin* is re-implemented twelve times across seven chores, as CSS selector strings of the form `plugin:has(groupId:containsWholeOwnText(X)):has(artifactId:containsWholeOwnText(Y))`. That is duplicated business logic, which the model classes as implicit [functional coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) — the most dangerous form short of intrusive, because the components need to change together and nothing says so.

A related leak sits on the YAML side: `GitHubActionsWorkflowFile` returns snakeyaml `Node` and `MappingNode` from `getJob`, `getStepByName` and `getOn`, so `AttestReleaseAssetsChore` passes library AST nodes around. The wrapper is good but its contract is not yet sealed.

### Complexity Impact

The cost is not hypothetical — the cascade has already happened once, in the other direction. Commit `7015b1d feat: upgrade to Jackson 3` was possible partly because Jackson was already behind `Jsons`/`JsonBuilder`. Had Jackson types been spread across fourteen chores the way jsoup types are, that upgrade would have been a fourteen-file change.

For the duplicated selectors, the cost is in making a change to how plugins are found. A fix — handling a `groupId` inherited from a parent pom, say, or whitespace inside the element — must be made in twelve places, and a missed one produces a chore that quietly fails to find a plugin it should have found.

### Cascading Changes

- **A jsoup major upgrade**, arriving automatically via Renovate, reaches into fourteen chores.
- **Improving Maven plugin detection** requires twelve coordinated edits with no compiler assistance, since the selectors are strings.
- **Switching XML libraries** — jsoup is an HTML parser doing duty as an XML one, and `doc.outerHtml()` is what writes poms back — is currently infeasible at any reasonable cost.

### Recommended Improvement

Finish the pattern the codebase has already established twice. Add a `MavenPomFile` in `tools`, alongside `GitHubActionsWorkflowFile` and `DependabotConfigFile`, exposing the operations the chores actually need: `findPlugin(groupId, artifactId)`, `insertPluginAfter(...)`, `property(name)`, `parentRelativePath()`, `asString()`. The twelve duplicated selectors collapse into one implementation of `findPlugin`, and jsoup stops appearing in `chores/` entirely.

Do this incrementally rather than as a single refactor: add `MavenPomFile`, migrate the four plugin chores (`EclipseFormatterPlugin`, `Spotbugs`, `Modernizer`, `MavenJavadocSources`) that share the most duplicated logic, and let the rest follow as they are touched. While there, seal the YAML wrapper's contract too — replace the `Node`-returning methods with a small `WorkflowStep`/`WorkflowJob` type so snakeyaml stops crossing the package boundary.

The trade-off is one more class and a period where both styles coexist. The precedent is strong: this is the same move that made the Jackson 3 upgrade tractable, applied to the format that did not get it.

---

## Issue 7: `setDirty()` is an implicit protocol that most chores do not follow

**Integration**: 31 file-writing chores → `ChoreContext.setDirty()` → `ChoritoCommand`'s refresh loop
**Severity**: Minor

### Knowledge Leakage

`ChoreContext` is otherwise a well-formed [contract](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/) — final fields, defensive copies, a builder, a `refresh` strategy injected as a function. One mutable boolean breaks the pattern: `setDirty()` mutates the context in place, and `ChoritoCommand` reads it to decide whether to re-scan the filesystem:

```java
currentContext = chore.doit(currentContext);
if (currentContext.isDirty()) {
    currentContext = currentContext.refresh();
}
```

The rule "if you create or delete files, tell the context" is documented nowhere and enforced by nothing. **31 chores write, move or delete files; 9 call `setDirty()`.** The other 22 leave the next chore with a `textFiles()`/`files()` list that no longer matches the filesystem.

### Complexity Impact

Mostly latent rather than active, which is why this is minor. Many of the 22 modify files in place rather than creating them, so the file list stays accurate. But a chore that creates a file without flagging it makes that file invisible to every subsequent chore in the run — `DirectoryStreams.githubWorkflows`, `mavenPoms` and the rest all filter `context.textFiles()`, so a freshly created workflow is simply not there. The symptom is a chore that appears not to run, and it resolves itself on the *next* invocation of chorito, which makes it hard to catch and easy to misdiagnose.

The [volatility](https://coupling.dev/posts/dimensions-of-coupling/volatility/) is in growth: every new chore is a fresh opportunity to forget, and the ratio suggests forgetting is the norm rather than the exception.

### Cascading Changes

- **Adding a chore that creates a file** without `setDirty()` silently starves every later chore of that file.
- **Reordering chores** changes which omissions matter, so a latent bug activates from an unrelated edit.

### Recommended Improvement

Remove the need to remember. `FilesSilent` already funnels every write, move and delete through a handful of methods; the cheapest fix is to have the context observe those operations rather than be told about them — for example by giving chores a context-scoped writer that marks dirty automatically when a path is created or removed.

If that is too large a change, the cheap alternative is to drop the optimisation: `refresh()` unconditionally after every chore. `PathChoreContext.refresh` walks the tree twice and `GitChoreContext.refresh` opens the repository, so there is a real cost — but it runs forty times on one repository, not in a hot loop, and correctness by construction is worth more than the saved walks here.

Either way, the goal is the same: make it impossible for a chore to be wrong by omission, rather than relying on 40 authors-in-sequence to remember an undocumented rule.

---

## Issue 8: The Maven plugin chain — unbalanced, and correctly left alone

**Integration**: `EclipseFormatterPluginChore` → `SpotbugsPluginChore` → `ModernizerPluginChore` → `MavenJavadocSourcesPluginChore`
**Severity**: Minor — recommended action is to accept

### Knowledge Leakage

Four chores form a prerequisite chain, each inserting its plugin immediately after the one its predecessor inserted, and each throwing if that predecessor's output is missing:

- `SpotbugsPluginChore`: `throw new IllegalStateException("No formatter plugin")`
- `ModernizerPluginChore`: `throw new IllegalStateException("No spotbugs plugin")`
- `MavenJavadocSourcesPluginChore`: `throw new IllegalStateException("No modernizer plugin")`

The shared knowledge — "my predecessor has already run and inserted its plugin" — is implicit [functional coupling](https://coupling.dev/posts/dimensions-of-coupling/integration-strength/), communicated through the pom file and enforced only by the order of the list in `ChoritoCommand`. By strength and distance alone this looks like the same problem as Issue 2.

### Complexity Impact

It is not, for two reasons, and both are worth stating because they are what stops this review from recommending a refactor here.

**Volatility is low.** Maven plugin conventions are a [supporting subdomain](https://coupling.dev/posts/dimensions-of-coupling/volatility/): necessary, but not where chorito competes, and not an area under active pressure. The git history bears this out — two commits each on `SpotbugsPluginChore` and `ModernizerPluginChore` in two years, against ten on `GitHubActionsWorkflowFile`. The [balance formula](https://coupling.dev/posts/core-concepts/balance/) — `(STRENGTH XOR DISTANCE) OR NOT VOLATILITY` — is satisfied by its second term. This is exactly the pragmatism the model builds in, and what Evans meant by "not all of a large system will be well designed."

**The chain is self-satisfying.** `EclipseFormatterPluginChore` does not fail when the formatter plugin is absent: it falls back to the flatten plugin, then to `doc.select("plugin").last()`, and inserts. So in the shipped order the chain always starts satisfied, and the three `IllegalStateException`s are unreachable in practice. They are assertions, not a live failure mode — and if one ever did fire, it would fire loudly and immediately rather than corrupting a pom.

### Cascading Changes

Realistically confined to one case: reordering `ChoritoCommand`'s list, or removing `EclipseFormatterPluginChore`, would break the chain. Both are deliberate acts on a stable area, and both fail fast with a message that names the missing plugin.

### Recommended Improvement

**Take no action.** Restructuring this would spend effort on a low-volatility area and buy nothing measurable.

One optional, near-free improvement: the exception messages already name the missing prerequisite, but nothing at the `ChoritoCommand` list says the order matters. A single comment above those four entries — noting that they must stay in this order and why — would record the knowledge where someone reordering the list would actually see it. That is the whole recommendation.

---

_This analysis was performed using the [Balanced Coupling](https://coupling.dev) model by [Vlad Khononov](https://vladikk.com)._
