# JvmDowngrader — Agent Notes

Gradle build (Kotlin DSL). Main source compiles for **Java 7**
(`mainVersion=7` in `gradle.properties`) — no Java 8+ APIs in
`src/main`/`src/shared` (no `computeIfAbsent`, no default interface methods,
no static methods on `@interface`). Test code targets whatever toolchain the
test JVM uses (Azul `stubToVersion`).

## Building & testing

- `./gradlew :testing:test` — unit tests + end-to-end downgrade matrix across
  every JDK from `testTargetVersion` to `stubToVersion` (Azul toolchains
  auto-provisioned via foojay-resolver).
- Tests run under JUnit 5 with **parallel `concurrent` execution** —
  tests that mutate JVM-global state (system properties, singletons) must be
  serialized (`@Execution(SAME_THREAD)`) or they will flake.
- CI: `.github/workflows/build.yml` and `build_snapshot.yml` run the full
  matrix in a `test` job before publishing.

### Test corpus layout

All corpus artifacts are source sets in `:testing` (no subprojects):

- **Feature corpus** (`src/featuretests/java/jvmdg/featuretests/`, modular
  jar): `@FeatureTest` probe methods + `Support.run(Class)` reflective
  dispatch; each corpus class's `main` prints `<id> RESULT:<v>`/`EXC:` lines
  so native vs downgraded/shaded output diffs line-for-line. Classes become
  batch mains via a `main`-named method or `@xyz.wagyourtail.TestMain`
  (needed when no `main` is declared, e.g. inherited compact mains).
  `CondyWoven` is emitted by `FeatureCorpusGenerator` (ASM-crafted ldc-condy
  javac can't produce) into `build/generated/featureClasses`, grafted into
  the module at compile time via `--patch-module`.
- **Stub-parity corpus**: manual probes in the **`stubtests` source
  set** (`src/stubtests/java`, IDE-visible) with `@StubTest(member =
  @Ref(...))`; bytecode fixtures javac can't emit in the desired shape
  (plain `makeConcat`, V65 `typeSwitch`) in **`stubtests21`**
  (`src/stubtests21/java`, compiled `--release 21 -XDstringConcat=indy` —
  named for the release it compiles at, so other input-version fixtures
  can follow as `stubtests<N>`); generated probes under
  `build/generated/stubtests` in the **`stubtestsGenerated`** source set
  (separate because `StubTestGenerator` reflects on the *compiled* manual
  classes — the two can't share one compile task). `StubTestGenerator`
  merges all three into the indexed parity corpus.
  - The `member` claim must use the **registered** desc — adapter return
    types are mapped to real JDK types at registration (e.g.
    `()Ljava/lang/ProcessHandle;`, not the `J_L_ProcessHandle` source type),
    and ids renumber whenever registrations change, so check
    `build/reports/stubtests/untestable.txt` for mismatched claims.
  - `stubparity-known-diffs.txt` —
    `<slug|all> <stable-ref> ==> <native-glob> ==> <downgraded-glob>` per
    line under a required rationale comment; the ref is the `index.txt`
    identity (`Lowner;name;desc@profile`, canonical
    `FullyQualifiedMemberNameAndDesc` form) so entries survive `m####`
    renumbering, and the `*`-wildcard globs pin the diff *content*
    (`\xNN`/`\uXXXX` escapes match control chars the corpus prints raw).
    Asserted in both directions: a diff that matches is fine, but an entry
    that now passes, fails to match its globs, has an unknown slug, or
    resolves to no corpus member **fails the suite**.
  - Reports land under `build/reports/stubtests/`: `index.txt` (id →
    member/profile map), `untestable.txt` (strict ratchet — members with
    no coverage path; should stay header-only), `deferred.txt` (registered
    members whose impls only activate below the tested matrix — the
    java 8→7 scope), `coverage.txt`, `inventory.txt` (full member
    registry).
- **Multi-release fixture** (`src/multiVersion/java`): `*J<N>` mains run on
  launchers ≥ N for `multiRelease*` flag coverage (`MultiVersionTests`).
- `jvmdowngrader$nest$*`, `jvmdg$indy$*`, `jvmdg$handle$*`,
  `jvmdg$methodtype$*`, `jvmdg$lookup`, `<Owner>$jvmdg$StaticDefaults`,
  `jvmdg$instanceMain` are emitted by `otherTransforms` — unit-tested in
  `TestOtherTransforms` with crafted ASM ClassNodes (no JVMs needed).

## commons-asm / commons-core mirrors

`wagyourtail/commons` contains forked copies of several classes. Fixes must be
ported both directions — the forks have diverged (commons uses sync
`walkFileTree`, lombok, `commons.core` Logger/utils) so ports adapt rather than
copy:

| JvmDowngrader | commons counterpart |
|---|---|
| `src/main/.../jvmdg/compile/shade/ReferenceGraph.java` | `commons-asm/.../commons/asm/graph/ReferenceGraph.java` |
| `src/main/.../jvmdg/asm/ASMUtils.java` | `commons-asm/.../commons/asm/ASMUtils.java` |
| `src/main/.../jvmdg/asm/AnnotationUtils.java` | `commons-asm/.../commons/asm/AnnotationUtils.java` |
| `src/shared/.../jvmdg/util/Utils.java` | `commons-core/.../commons/utils/Utils.java` |
| `src/main/.../jvmdg/classloader/ResourceClassLoader.java` | `commons-core/.../commons/classloader/ResourceClassLoader.java` |
| `src/shared/.../jvmdg/util/Lazy.java` | `commons-core/.../commons/lazy/Lazy.java` |
| `src/shared/.../jvmdg/util/AsyncUtils.java` | `commons-java7/.../commons/java7/AsyncUtils.java` |
| `src/main/.../jvmdg/collection/*Enumeration.java` | `commons-core/.../commons/enumerations/*Enumeration.java` |

commons tests are JUnit 5 on JDK 17 (`autoToolchain` per module). Verify ports
with `./gradlew :commons-asm:test` (or the relevant module) in the commons repo.

## Compatibility constraints

- `VersionProvider.priotity` is a **published misspelled field** — keep it;
  use the `priority` alias internally. Do not rename/remove.
- The agent must never `System.exit` — use `IllegalClassFormatException`.
- `src/shared` is compiled into the api jar — changes there affect consumers.
