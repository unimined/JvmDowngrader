# Contributing to JvmDowngrader

So, you've decided to contribute? Great! This covers dev setup, project
layout, the dev loop, and the conventions PRs are expected to follow. If
you want the deep version of how the machinery fits together, read
[docs/Architecture.md](docs/Architecture.md) and the root
[AGENTS.md](AGENTS.md) — this file stays practical.

## Dev setup

1. Clone the repository.
2. Build with `./gradlew build`.

Gradle auto-provisions every JDK the build needs via the
[foojay-resolver](https://github.com/gradle/foojay-toolchains) toolchain
plugin (the test matrix spans `testTargetVersion`→`stubToVersion`,
currently 8→27) — no manual installs needed in the normal case. Gradle
itself needs **JDK 11+** to run; point `org.gradle.java.home` or your IDE's
Gradle-JVM setting at one if your default `java` is older. If a toolchain
can't be provisioned, see the [troubleshooting](#troubleshooting) notes.

## Project layout

- `src/main` + `src/shared` — the core (CLI, downgrader, pipeline, shading).
  Compiles on the `stubToVersion` (27) toolchain, then **self-downgrades to
  Java 8 bytecode** before packaging — the shipped artifact runs on a Java
  8+ runtime. Sources stay Java 8 in practice: `compileMainV52`/
  `compileSharedV52` run real javac-8 compiles of the same sources for the
  pre-27 consumer classpaths (java-api, gradle-plugin, javac-plugin), so
  lambdas/streams/`computeIfAbsent` are fine but Java 9+ APIs are not.
  `src/shared` also ships inside the api jar — keep it dependency-free and
  ASM-free.
- `java-api/` — the stub implementations for newer Java APIs, one source set
  per Java version (`src/java<N>/java`).
- `gradle-plugin/` — the Gradle plugin (`xyz.wagyourtail.jvmdowngrader`).
- `javac-plugin/` — the javac plugin facade.
- `testing/` — unit tests plus the end-to-end corpus (see AGENTS.md for the
  corpus layout).

The root project exports a `-all` jar (the CLI artifact). Use
`publishToMavenLocal` when testing a fix consumed by another project.

## Build & test commands

- `./gradlew build` — full build; also exercises the self-host downgrade
  and `selfHostSmokeTest`.
- `./gradlew :testing:test` — the parity gate: unit tests plus the
  end-to-end downgrade/shade matrix across every JDK from
  `testTargetVersion` to `stubToVersion`. Thorough and slow — use focused
  invocations while iterating.
- `./gradlew :testing:test --tests "*TestStructuralRemapTransform*"` —
  focused test runs. Corpus jars land in `testing/build/libs/` for manual
  inspection; shaded/downgraded intermediates under
  `testing/build/tmp/test/`.
- `./gradlew selfDowngradeCore selfDowngradeShared` — runs this build's own
  ZipDowngrader over the raw main/shared jars against the self-host api
  jar, emitting `build/selfhost/{core,shared}-downgraded-8.jar`. This is
  the dogfood path — it's what ships.
- `./gradlew selfHostSmokeTest` — runs the fat jar's `Main --version` on a
  Java 8 launcher; wired into `check`.
- `./gradlew :java-api:downgradeJar8Exec` — downgrades the api jar to v52
  (what the shipped jars embed for standalone `-jar` runs).
- `./gradlew :java-api:genStubIndex` — regenerates `META-INF/jvmdg/stubs.idx`
  plus the generated dispatch products (retro/gained helpers, marker
  interfaces). Run this after changing stub registrations.
- `./gradlew :java-api:coverageReport` — diffs adjacent `ct.sym` surfaces
  per boundary into `java-api/build/coverage/<version>/`; see
  [docs/Coverage.md](docs/Coverage.md).

## The dev loop

Normal single-tree flow — no special worktree setup needed.

- **Stub work** (new/changed stub in `java-api/`): edit →
  `./gradlew :java-api:genStubIndex` → focused `:testing:test` runs
  (`--tests "*StubParity*"`, `--tests "*TestStubRegistration*"`) → the full
  matrix before merge.
- **Transform work** (`src/main`): edit → focused unit tests →
  `./gradlew selfDowngradeCore selfDowngradeShared selfHostSmokeTest` to
  prove the self-host path still works → `:testing:test` before merge.
- Output-affecting changes (stub registrations, transform behavior) need a
  corpus probe or a `stubparity-known-diffs.txt` entry — see below.

## The test corpus

All corpus artifacts are source sets in `:testing`; the full map is the
corpus section of [AGENTS.md](AGENTS.md). Short version:

- `src/featuretests/` — `@FeatureTest` probes; each corpus `main` prints
  `<id> RESULT:<v>`/`EXC:` lines so native vs downgraded output diffs
  line-for-line. Set `-Djvmdg.corpus.debugExc` on the run JVM to also dump
  `EXC:` stack traces.
- `src/stubtests/` — manual stub-parity probes (`@StubTest(member =
  @Ref(...))`; the member claim must use the **registered** desc).
- `src/stubtests21/` — bytecode fixtures javac can't emit in the desired
  shape, compiled `--release 21 -XDstringConcat=indy`.
- Generated probes (`build/generated/stubtests`) plus
  `src/multiVersion/` fixtures for the multi-release flags.
- Reports land under `build/reports/stubtests/`: `index.txt`,
  `coverage.txt`, `untestable.txt` (strict ratchet — stays header-only),
  `deferred.txt`. Intentional behavior diffs get a line in
  `stubparity-known-diffs.txt` (`<slug|all> <stable-ref> ==> <native-glob>
  ==> <downgraded-glob>`) under a required rationale comment — the suite
  fails on entries that pass, don't match, or name unknown members.

## Debugging flags worth knowing

Full list in [docs/Configuration.md](docs/Configuration.md); these come up
most when working on the transforms themselves:

- `-Djvmdg.debug.dumpClasses` / `--dumpClasses` — dumps transformed classes
  to `.jvmdg/debug/` for `javap` inspection.
- `-Djvmdg.debug.noStubIdx` — forces the reflective stub scan instead of
  loading `stubs.idx`; bisects index-vs-scan differences.
- `--logLevel debug` (sysprop `jvmdg.logLevel`) and `--noColor`
  (`jvmdg.logAnsiColors`) — verbose, paste-able logs.
- `--noIndy` — emits zero `invokedynamic`/`ConstantDynamic`; exercises the
  desugar/lowering path end-to-end.
- `--ignoreWarningsIn <glob>` — silences missing-stub warnings for matched
  classes during exploratory runs.
- `-Djvmdg.debug.noFuse` — disables transform fusion; the sequential path
  is the correctness oracle when a fused output looks wrong.

## Conventions

The hard rules live in
[docs/AGENTS-CONTRIBUTING.md](docs/AGENTS-CONTRIBUTING.md); the highlights:

- Match the surrounding code style; feature branches off `main` (or the
  current stacked feature branch — check with the maintainer).
- **Do not copy JDK source** into stubs — LGPL constraints mean every stub
  is a reimplementation. See [docs/Stubs.md](docs/Stubs.md) for the
  authoring guide and [docs/Bytecode
  Manipulation.md](docs/Bytecode%20Manipulation.md) for what each
  registration kind does to the bytes.
- Java 8 floor for `src/main`/`src/shared` (the javac-8 compile gates
  above are real). `src/shared` ships in the api jar: no ASM, no
  dependencies, nothing that can't run on a bare JRE.
- **Never `COMPUTE_FRAMES`** — hand-author `FrameNode`s or leave frames
  untouched; writers use `COMPUTE_MAXS` only.
- **Never `System.exit`** in agent/classloader paths — throw
  `IllegalClassFormatException` (or propagate) instead.
- Determinism is a feature: sorted output, pinned timestamps, generated
  `jvmdg$…`/`jvmdowngrader$…` names that are pure functions of
  (class, site) — no global counters.
- Runtime-reachable output must not reference `org/objectweb/asm/**`.
- Tests run under JUnit 5 with `parallel`/`concurrent` execution —
  anything mutating JVM-global state (system properties, singletons) needs
  `@Execution(SAME_THREAD)` or it will flake.

## PR expectations

- Focused commits; match the repo's existing commit style.
- `./gradlew :testing:test` is green — the matrix is the parity gate, and
  CI (`.github/workflows/build.yml`) runs the full matrix on every PR.
- Behavior changes come with tests: a corpus probe or a
  `stubparity-known-diffs.txt` entry (rationale comment required) for
  intentional output diffs.
- `build/reports/stubtests/untestable.txt` stays header-only — it's a
  strict ratchet.
- Architecture or flag changes update [AGENTS.md](AGENTS.md) and the
  affected doc under `docs/` in the same PR.

## License

JvmDowngrader is LGPL-2.1-or-later — see [LICENSE.md](LICENSE.md) and the
license section of the README for what shading/downgrading means for your
own jar.

## Troubleshooting

### Toolchain problems

`src/main`/`src/shared` compile on a JDK 27 toolchain (`stubToVersion`) and
self-downgrade to `mainVersion=8`; the `java-api` per-version source sets
each use the toolchain of the version they downgrade toward (java7→
`--release 6`, java8→`--release 7` on a JDK 11 compiler, java9+ → that
version's own JDK). foojay-resolver provisions Azul toolchains
automatically; on machines where provisioning fails, install the matching
JDK and point Gradle at it (`org.gradle.java.installations.paths`).

### Provisioned toolchain could not be probed

Usually a broken JDK install cached by Gradle — delete the offending
toolchain under `GRADLE_HOME/jdks` and retry, or install the version named in
the error and change the vendor in [java-api/build.gradle.kts](java-api/build.gradle.kts)
(`javaToolchains` blocks) if provisioning keeps failing.

### `No matching variant of com.gradleup.nmcp` errors

Gradle needs JDK 11+ to run. Set the Gradle JVM — in IntelliJ: Gradle tool
window → ⚙ → "Gradle JVM" → pick JDK 11+.
