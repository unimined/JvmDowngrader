# JvmDowngrader — Agent Notes

Gradle build (Kotlin DSL). Main source compiles for **Java 8**
(`mainVersion=8` in `gradle.properties`) — Java 8 language/APIs are fine in
`src/main`/`src/shared`; core/agent/CLI then require a Java 8+ **runtime**,
while downgrade *targets* still go down to 7/6/5 (the 8→7 stub work is
separate and deferred). Test code targets whatever toolchain the test JVM
uses (Azul `stubToVersion`).

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

## Architecture map (3.0)

The core is a **version-agnostic pipeline** with registrars layered on top:

```
entry: PathDowngrader / ZipDowngrader / ApiShader / CLI Main / Gradle / javac
   └─ compile/jar/   JarStep chain in one FusedJarRun traversal
        EmbeddedRulesStep (META-INF/jvmdg/*.mf scan, sandboxed apply, strip)
          → SignatureStripStep → ManifestStep → MultiReleaseRouteStep
          → ClassDowngradeStep (per-entry, legacy path)
          → or PipelineJarStep (whole-set: downgrade union + shade in one run)
             └─ pipeline/    version-free engine
                  TransformPipeline: ordered Stage list over a name-keyed
                  TransformWorklist; Stage.filter gates per-class-version,
                  captureLabels snapshots into ctx.variants, onEnter/onExit
                  are StageHooks (barriers); ExtrasApplyTransform applies
                  stubWithExtras products; TransformException carries
                  stage/transform/class provenance
             └─ version/     DowngradeRegistrar emits the stage list
                  per-VersionProvider boundary (decomposed version/steps/:
                  ProviderInit → StubClasses → StubMethods → InsertAbstracts
                  → OtherTransforms → FlushWarnings; ProviderStep retained
                  only for providers overriding downgrade()). stages(Set)
                  builds the union chain for mixed-version input sets.
             └─ version/map/ StubRegistry (per-owner store) + StubScanner
                  (reflective @Stub/@Modify/@Adapter registration) +
                  MemberStubTransform (emission) + StructuralRemapTransform
                  (mechanical type-slot remap) + MemberResolver/TypeRemap
                  (ClassMapping implements both — resolver graph only)
             └─ version/idx/ StubIndex — META-INF/jvmdg/stubs.idx codec
                  (deterministic, format+jvmdg-versioned); StubIndexWriter
                  serializes the reflective scan at api-jar build time
                  (genStubIndex JavaExec; also stamps memberSince per
                  registered member from the genCtSym JDK-surface index);
                  providers idx-load at ensureInit, falling back to the
                  scan (jvmdg.debug.noStubIdx forces it)
             └─ stub kinds + policy — memberSince/memberKind side-maps on
                  StubRegistry (idx-populated; scan path leaves memberSince
                  empty → MISSING_API); kindOf = kind= override → modify ⇒
                  LANGUAGE → since≤target ⇒ BEHAVIOR → else MISSING_API.
                  MemberStubTransform.vetoed applies Stub-Policy
                  (full/noBehavior/mechanical) + On-Missing-Api (error/warn)
                  and scoped exclusions post-resolution — never on
                  unstubbed calls
             └─ compile/shade/ ReferenceIndex (member-level edges, floor
                  slot lookup, usages inversion, BFS closure) + IndexScanner
                  (entry stream → index) + RefExtractor SPI (MemberRefExtractor,
                  RequiresResourceScanner, ReflectionReferencesScanner) +
                  ShadeRegistrar stages: scan → closure-barrier (required set,
                  inline candidates, per-slot remappers, pruned+prefixed api
                  ClassNodes, api resources, variant shading) → inline →
                  prefix → module-info-strip
asm/index/   ClasspathIndex/NamespaceView/WorkingClasses — memoized
             classpath hierarchy queries (replaces re-parse amplification)
asm/info/    ClassInfo/MethodInfo/FieldInfo — member projections
config/      BoundFlag (field annotation) + FlagBindings (one reflective
             index driving sysprop init, CLI gen/apply, config keys,
             copy/equals/hash/toString, --print, Gradle mapping) +
             ManifestConfigReader (jvmdg.mf syntax) + JvmdgConfig (application
             rules: version header, unknown keys, scoped sections) +
             ScopedRule (Name:-scoped, jarScope-sandboxed, most-specific wins;
             typed accessors: ignoreWarnings/excludeStubs/excludeJeps/
             stubPolicy/onMissingApi) + StubPolicy + OnMissingApi enums
version/idx/ StubIndex codec + StubIndexWriter — deterministic, versioned
             stubs.idx shipped in the api jar; ensureInit loads it instead of
             reflectively scanning (~5x faster provider init)
classloader/ DowngradingClassLoader — runtime transform path (per-class
             downgrade, untouched by the jar-level rework)
```

Invariants:

- **Java 8 baseline** in `src/main`/`src/shared` (`mainVersion=8`) — lambdas,
  streams, `computeIfAbsent`, `@Repeatable`, default/static interface methods
  are all allowed. The jvmdg functional interfaces in
  `src/shared/.../util` (`Function`, `Consumer`, `IO*`) remain for output-side
  compatibility until the deferred 8→7 work; don't add new uses where a
  `java.util.function` type fits naturally.
- **No `COMPUTE_FRAMES`, ever.** Transforms hand-author `FrameNode`s; the
  shape-preserving contract keeps them valid so downstream consumers (incl.
  the deferred frame-analysis channel) can trust them.
- **Deterministic output**: sorted writes, pinned timestamps
  (`OutputUtils.pinTimestamps`), no global counters in synthesized names —
  every `jvmdg$`/`jvmdowngrader$` name is a pure function of (class, site).
- Runtime output stays **ASM-free**; ASM is compile-side (or shaded input)
  only.
- `VersionProvider.priotity` is a published misspelled field — keep it.
- The agent never `System.exit`s — use `IllegalClassFormatException`.
- `src/shared` ships inside the api jar — no heavy deps, no ASM.

## Extension-point cookbook

| To add a… | Do this |
|---|---|
| Stub member | `public static` method + `@Stub` on a `stub(Class)`-registered class in the right `java-api/src/java<N>` source set; `StubScanner` registers it at provider `ensureInit`. `member` claims use the *registered* desc (adapter return types remap to real JDK types). |
| Insn-level rewrite | `@Modify(ref = @Ref("Lowner;name;desc"))` on a `public static` method taking `(MethodNode, int, ClassNode, Set<ClassNode>, Object…)` — emits replacement insns, may enqueue extras. |
| Class-type redirect | `@Adapter` — feeds `TypeRemap`/`StructuralRemapTransform`; `keepInterface` drops the type from `implements`. |
| New java version | New `Java<N>Downgrader` under `providers/` + `java-api/src/java<N>` source set + `META-INF/services` registration; `downgradeTo` chains it automatically. |
| Wider per-class pass | `otherTransforms` on the provider — becomes `OtherTransformsTransform` in the stage; emits `jvmdg$indy$/handle$/methodtype$/lookup/nest$/instanceMain` helpers via `ctx.extras`. |
| Whole-set/ordering-sensitive pass | A `StageHook` (`onEnter`/`onExit`) — sees the full `TransformContext`; the shade closure barrier is the model. |
| Ref kind for the index | A `RefExtractor`/`MemberScanner` SPI impl — plug into `ReferenceIndex` construction; `RequiresResourceScanner` is the model. |
| Flag / config option | `@BoundFlag` on the `Flags` field — CLI/sysprop/config/`copy()`/`--print`/Gradle application come free; Gradle needs only the typed `Property` on `DowngradeFlags` (`toFlags` resolves bound `gradle` names reflectively, values convert via the flag's `form` parser). Docs table is drift-tested (`everyBoundKeyAndCliFlagIsDocumented`), the property surface by `TestToFlags`. See `docs/Configuration.md`. |

## Verification checklist

- `./gradlew :testing:test` — full matrix must stay green (unit + integration
  + stub-parity diff corpus). `stubparity-known-diffs.txt` only changes
  deliberately.
- Byte-parity when touching transforms: run the corpus through the new path
  vs the old (or fused vs `--debugNoFuse`) and diff jar entries + corpus
  output lines.
- Async safety: any collection mutated inside `visitPathsAsync`/`forEachAsync`
  callbacks must be concurrent (`ConcurrentHashMap`-backed) — a plain
  `HashSet`/`HashMap` silently loses entries.
- Public API additions get unit tests under `testing/src/test`.

## commons-asm / commons-core mirrors

`wagyourtail/commons` contains forked copies of several classes. Fixes must be
ported both directions — the forks have diverged (commons uses sync
`walkFileTree`, lombok, `commons.core` Logger/utils) so ports adapt rather than
copy. The full which-side-is-truth table is in
[docs/commons-mirror.md](docs/commons-mirror.md):

| JvmDowngrader | commons counterpart |
|---|---|
| `src/main/.../jvmdg/compile/shade/ReferenceIndex.java` (was `ReferenceGraph`) | `commons-asm/.../commons/asm/graph/ReferenceGraph.java` (replaced by the index trio) |
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
