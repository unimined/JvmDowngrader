# Changelog

## 3.0.0 (unreleased)

The core was rebuilt around a version-agnostic transform pipeline. Output is
byte-parity with 2.x for supported inputs; the changes below are architecture
and API surface, not behavior.

### Architecture

- **Pipeline engine** (`xyz.wagyourtail.jvmdg.pipeline`) — ordered `Stage`s
  over a name-keyed `TransformWorklist`: per-stage transforms, `onEnter`/
  `onExit` `StageHook` barriers, `precondition`/`filter` version gating,
  `captureLabels` variant snapshots, `ExtrasApplyTransform` worklist re-entry,
  `FusibleTransform` adjacent-stage composition (`--debugNoFuse` opt-out),
  `TransformException` with stage/transform/class provenance, `DiagnosticSink`
  dedup'd warning drains.
- **`DowngradeRegistrar`** decomposes each `VersionProvider` boundary into
  `version/steps/` transforms (init → stub classes → stub methods → insert
  abstracts → other transforms → flush warnings); a single
  `stages(Set<Integer>)` union chain handles mixed-version inputs via
  per-stage filters.
- **Stub model split** — `StubRegistry` (per-owner store) + `StubScanner`
  (reflective registration) + `MemberStubTransform` (emission) +
  `StructuralRemapTransform` (mechanical type-slot remap); hierarchy queries
  resolve through the memoized `asm/index/ClasspathIndex` +
  `NamespaceView`/`WorkingClasses` and `asm/info/ClassInfo` projections.
- **`ReferenceIndex`** replaces `ReferenceGraph` — member-level reference
  edges, floor-based multi-release slot lookup, usage inversion, BFS closure;
  pluggable `RefExtractor`/`MemberScanner` SPI.
- **Shading in-pipeline** — `ShadeRegistrar` appends scan → closure-barrier →
  inline → prefix → module-info stages onto the same run.
- **Jar-level fused steps** (`compile/jar/`) — `JarStep`/`FusedJarRun`/
  `JarEntrySink` one-traversal composition; `PipelineJarStep` runs the
  downgrade union + shading over the whole class set in a single pipeline.
  `Main` fuses `downgrade - shade` invocations chained through `-` into one
  pass.

### Added

- **Declarative constructor annotation family** — `<init>` handling moved off
  `Stub`/`@Modify` onto dedicated, repeatable annotations registered into a
  per-owner `ctor` map (`CtorEntry`): `NEW`/`DUP` allocation sites and
  `this()`/`super()` delegation are verifier-gated; unprovable shapes warn and
  leave the original call; malformed registrations fail at scan time.
  - `@InitRewrite(ref = @Ref("Lowner;<init>;desc"), to = "desc", keep = {…})`
    (class-level, repeatable — `@InitRewrites` boxes below Java 8) retargets
    a constructor: `invokespecial` calls the `to` constructor of the same
    class with `keep`-mapped args. Arg surgery spills originals to fresh
    locals (eval-order preserving, category-2 safe) instead of hand-emitted
    stack shuffles.
  - `@InitArg` / `@InitPre` / `@InitPost` (method-level, repeatable —
    `from`/`owner` disambiguate overloads) supply arg converters, pre-init
    hooks, and post-init tails `(ConstructedType, keptArgs…)` per rewrite.
  - `@InitFactory` replaces construction with a static factory —
    `new X(args)` becomes `X stub(args)`. Delegation shapes are left
    unchanged (an uninitialised `this` can't be passed out).
  - `X::new` method references and other `invokedynamic` impl handles
    (`H_NEWINVOKESPECIAL`) route through the same ctor map: a `jvmdg$handle$`
    wrapper replays the rewrite or calls the factory, so constructor
    method-refs downgrade correctly instead of failing linkage with
    `BootstrapMethodError`.
  - All 26 `<init>` `@Modify`/`@Stub` handlers in `java-api` migrated;
    `Stub.init`/`Stub.args` removed. `J_L_IndexOutOfBoundsException`'s
    `(long)` stub emits explicit `StringBuilder` concat so the api jar stays
    `invokedynamic`-free.
- **`META-INF/jvmdg/stubs.idx`** (`version/idx/`) — the api jar ships a
  deterministic, format- and jvmdg-versioned binary index of every
  provider's stub registry, written at build time by `genStubIndex` from
  the reflective scan's output. `VersionProvider.ensureInit` loads the
  index instead of reflectively scanning stub classes (measured ~5x faster
  provider init); the scan remains the semantic source of truth and the
  parity oracle (`indexMatchesReflectiveScan` field-compares idx-loaded vs
  scanned registries per provider). `jvmdg.debug.noStubIdx` forces the
  scan; an absent index or missing provider section falls back
  transparently (this also covers shaded api jars, whose provider names no
  longer match the index keys), while a malformed or version-mismatched
  index fails fast. `debugSkip*` filters apply identically on both paths.
- **`jvmdg.mf` config files + annotation-bound flags** (`config/`) — the
  `Flags` option surface is declared once per field via `@BoundFlag`; that
  single source drives system-property init, CLI flag declaration and
  application, config-file keys, `copy()`/`equals`/`hashCode`/`toString`,
  the Gradle DSL mapping, and the `--print` dump — nothing is hand-mirrored
  (a field without binding metadata fails class-init). `jvmdg.mf` is JAR
  manifest syntax (plus ordered sections, duplicate rejection, no length
  limit, `file:line` errors): `Jvmdg-Config-Version: 1` header, global keys
  in the main section, `Name:`-scoped sections resolved most-specific-wins
  (equal-specificity conflicts are errors). Applied via `--config` (CLI,
  repeatable, ordered), `jvmdg { config.from(...) }` (Gradle,
  `@InputFiles`-cache-tracked), and the javac plugin's shared args.
  Precedence: defaults < embedded rules < config files < sysprops < CLI/DSL.
  `jvmdg config --print` / `jvmdgPrintConfig` dump the merged state with
  per-value sources. `Shade-Prefix` supplies `--prefix`/Gradle `shadePath`
  defaults. See `docs/Configuration.md`.
- **Embedded jar rules** — input jars carrying `META-INF/jvmdg/*.mf` are read
  before their classes transform; every rule is sandboxed to the jar's own
  package set (named sections intersect it too), global/unknown keys warn and
  are ignored so a dependency's rules can't steer or brick your build. Rules
  strip from output by default; `Keep-Embedded-Rules` preserves them,
  `Ignore-Embedded-Rules` skips application entirely and passes files
  through. First consumable scoped key: `Ignore-Warnings` per matched class.

### Breaking API changes

- **Java 8 runtime floor** — `mainVersion` bumped 7→8: the core jar, CLI,
  agent (`-javaagent`), `DowngradingClassLoader`, and Gradle/javac plugins
  now require a Java 8+ JVM to *run*. Downgrade *targets* are unaffected —
  output still goes down to 7/6/5 (8→7 stubs remain deferred). A
  self-downgraded runtime artifact is planned follow-up work.
- `ReferenceGraph` deleted — use `compile/shade/ReferenceIndex`.
- `ClassMapping` reduced to a `MemberResolver`/`TypeRemap` implementation;
  registration/emission moved to `StubRegistry`/`StubScanner`/
  `MemberStubTransform`/`StructuralRemapTransform`.
- Registry records are now declarative: `StubEntry`/`CtorFactory`/
  `CtorRewrite`/`memberModify` carry `MethodRef` (lazy `Method`
  resolution), and `classStubs` maps to `ClassStubEntry` instead of
  `Pair<Class<?>, Adapter>` — both serializable for `stubs.idx`.
- `VersionProvider.downgrade` no longer drives the whole class transform —
  providers emit stage content; the registrar composes it. Providers
  overriding `downgrade` still run via `ProviderStep`.
- `otherTransforms` gained a full-signature overload
  `(clazz, extra, getReadOnly, warnings, enableRuntime, memberResolver, superTypeResolver)`;
  narrower overloads delegate down.
- `@Modify` handler parameters are subsequence-matched against
  `MODIFY_SIG` `(MethodNode, int, ClassNode, Set, boolean)` — the fifth arg
  mirrors `debugNoSynthetic` for generated members.
- `ApiShader`'s shading internals moved to `compile/shade/` (the public
  `shadeApis` signatures are unchanged; the version-loop is one
  `FusedJarRun` + `PipelineJarStep` call).

### Fixed

- Nondeterministic api-class retention in `shadeApis` — `apiClasses` was a
  plain `HashSet` mutated inside the async path walk, dropping entries and
  intermittently producing shaded jars missing api classes
  (`NoClassDefFoundError` at runtime).
- `multiReleaseOriginal` entries are no longer sent through the downgrade
  transforms — they pass through verbatim like 2.x.
- Versioned `module-info.class` entries are dropped as 2.x did; jars with
  only `META-INF/versions/` classes no longer fail.

### Notes

- Java 8→7 and lower stub coverage is still deferred — the supported floor
  remains Java 8.
- `VersionProvider.priotity` remains a published misspelled field.
