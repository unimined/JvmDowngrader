# Contributing to JvmDowngrader

So, you've decided to contribute? Great! This covers dev setup, project
layout, and the conventions PRs are expected to follow.

## Dev setup

1. Clone the repository.
2. Build with `./gradlew build`.

Gradle auto-provisions every JDK the build needs via the
[foojay-resolver](https://github.com/gradle/foojay-toolchains) toolchain
plugin (the test matrix spans JDK 8→`stubToVersion`, currently 27) — no
manual installs needed in the normal case. Gradle itself needs **JDK 11+**
to run; point `org.gradle.java.home` or your IDE's Gradle-JVM setting at one
if your default `java` is older. If the `mainVersion=7` compile toolchain
can't be provisioned, see the [troubleshooting](#troubleshooting) notes.

## Project layout

- `src/main` + `src/shared` — the core (CLI, downgrader, pipeline, shading).
  Compiles for **Java 8** (Java 8 language/APIs fine). `src/shared` also
  ships inside the api jar — keep it dependency-free and ASM-free.
- `java-api/` — the stub implementations for newer Java APIs, one source set
  per Java version (`src/java<N>/java`).
- `gradle-plugin/` — the Gradle plugin (`xyz.wagyourtail.jvmdowngrader`).
- `javac-plugin/` — the javac plugin facade.
- `testing/` — unit tests plus the end-to-end corpus (see AGENTS.md for the
  corpus layout).

The root project exports a `-all` jar (the CLI artifact). Use
`publishToMavenLocal` when testing a fix consumed by another project.

## Conventions

- Match the surrounding code style; feature branches off `main` (or the
  current stacked feature branch — check with the maintainer).
- **Do not copy JDK source** into stubs — LGPL constraints mean every stub is
  a reimplementation. See
  [docs/Bytecode Manipulation.md](docs/Bytecode%20Manipulation.md) for the
  stub-authoring guide.
- Determinism is a feature: sorted output, pinned timestamps, generated names
  that are pure functions of (class, site).

## PR expectations

- `./gradlew :testing:test` is green — the matrix is the parity gate.
- Stub registration or transform changes that alter output come with a corpus
  probe or a `stubparity-known-diffs.txt` entry (rationale comment required).
- `build/reports/stubtests/untestable.txt` stays header-only — it's a strict
  ratchet.
- Targeted tests: `./gradlew :testing:test --tests "TestClassName"`; the
  corpus jars land in `testing/build/libs/` for manual inspection, and
  shaded/downgraded intermediates under `testing/build/tmp/test/`.

## License

JvmDowngrader is LGPL-2.1-or-later — see [LICENSE.md](LICENSE.md) and the
license section of the README for what shading/downgrading means for your
own jar.

## Troubleshooting

### Toolchain problems

`src/main`/`src/shared` compile with a JDK 8 toolchain (`mainVersion=8`);
the `java-api` per-version source sets each use the toolchain of the version
they downgrade toward (java7→`--release 6`, java8→`--release 7` on a JDK 11
compiler, java9+ → that version's own JDK). foojay-resolver provisions Azul
toolchains automatically; on machines where provisioning fails, install the
matching JDK and point Gradle at it (`org.gradle.java.installations.paths`).

### Provisioned toolchain could not be probed

Usually a broken JDK install cached by Gradle — delete the offending
toolchain under `GRADLE_HOME/jdks` and retry, or install the version named in
the error and change the vendor in [java-api/build.gradle.kts](java-api/build.gradle.kts)
(`javaToolchains` blocks) if provisioning keeps failing.

### `No matching variant of com.gradleup.nmcp` errors

Gradle needs JDK 11+ to run. Set the Gradle JVM — in IntelliJ: Gradle tool
window → ⚙ → "Gradle JVM" → pick JDK 11+.
