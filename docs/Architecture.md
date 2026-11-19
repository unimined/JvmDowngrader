# JvmDowngrader architecture

This document explains how JvmDowngrader downgrades Java class files across versions — the pipeline machinery, the
version-boundary registrars, the stub model, and the runtime dispatch mechanisms that keep downgraded bytecode working.
It is written for contributors and advanced users; every mechanism described here is backed by current source, and the
links point at it.

## What it does

JvmDowngrader reads classes compiled for a newer Java version and rewrites them so they load and behave correctly on an
older JVM — down to Java 8 as a runtime floor, with lower targets (7/6/5) reachable in the transform chain. Two kinds of
parity drive the design:

- **Signature parity** — the downgraded class answers reflection and linkage queries the way the authored class would
  have on the newer JDK. Types the target JVM lacks are remapped to stand-ins, callsites are rerouted, and
  `@JvmDowngraded` provenance keeps the authored version visible at runtime.
- **Behavioral parity** — where the JDK added a member or changed semantics, a registered stub implementation takes over
  at the callsite so behavior follows the newer contract. Where parity is impossible, the machinery fails loudly
  (`AbstractMethodError`, `ClassNotFoundException`, `NoSuchMethodException`) rather than silently degrading.

Entry points into the same machinery:

| Entry point | Role |
| --- | --- |
| [`PathDowngrader`](/src/main/java/xyz/wagyourtail/jvmdg/compile/PathDowngrader.java) | Downgrades a list of input roots (dirs or zips) to output roots |
| [`ZipDowngrader`](/src/main/java/xyz/wagyourtail/jvmdg/compile/ZipDowngrader.java) | Single-zip convenience over `PathDowngrader`; also the self-host tool |
| [`ApiShader`](/src/main/java/xyz/wagyourtail/jvmdg/compile/ApiShader.java) | Downgrade + shade: emits only the api closure the input needs, prefixed/inlined |
| [`cli.Main`](/src/main/java/xyz/wagyourtail/jvmdg/cli/Main.java) | Command-line front end over the above |
| [`JVMDowngraderPlugin`](/gradle-plugin/src/main/kotlin/xyz/wagyourtail/jvmdg/gradle/JVMDowngraderPlugin.kt) | Gradle plugin (`DowngradeJar`, `ShadeJar`, `DowngradeFiles`, `ShadeFiles`, `PrintConfig`) |
| [`JvmdgJavacPlugin`](/javac-plugin/src/main/java/xyz/wagyourtail/jvmdg/javac/JvmdgJavacPlugin.java) | javac `-Xplugin` hook that downgrades as part of compilation |
| [`runtime.Bootstrap`](/src/main/java/xyz/wagyourtail/jvmdg/runtime/Bootstrap.java) / [`classloader`](/src/main/java/xyz/wagyourtail/jvmdg/classloader/) | Runtime delivery: standalone launcher, `-javaagent`, downgrading classloader |

All of them converge on one [`ClassDowngrader`](/src/main/java/xyz/wagyourtail/jvmdg/ClassDowngrader.java)
(`downgradeTo(int)` / `downgradeTo(Flags)`), which owns the provider chain and the
`downgrade(name, bytes, enableRuntime, getExtraRead)` transform.

## The layout at a glance

```
compile/jar/      one fused jar traversal — JarStep chain over a JarEntrySink
pipeline/         version-agnostic engine: Stage + ClassTransform over a name-keyed TransformWorklist
version/          the downgrade registrar: VersionProvider per boundary, stage decomposition, annotation SPI
version/map/      stub registries, member/type resolvers, emission transforms
version/idx/      stubs.idx codec + build-time derivation artifacts
version/gen/      api-jar build-time codegen (dispatchers, markers, helpers)
compile/shade/    reference index + closure-based shading inside the pipeline
asm/index/        classpath indexing behind member/hierarchy resolution
stub/ (shared)    ASM-free runtime: dispatch bootstrap, runtime tables, reflection table — everything output calls
classloader/,     runtime delivery: DowngradingClassLoader, agent, bootstrap
runtime/
```

The engine in [`pipeline/`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/) knows nothing about Java versions;
[`version/`](/src/main/java/xyz/wagyourtail/jvmdg/version/) supplies the version chain as a `PipelineRegistrar`. Anything
downgraded output calls at runtime lives under [`src/shared/java/xyz/wagyourtail/jvmdg/stub/`](/src/shared/java/xyz/wagyourtail/jvmdg/stub/)
and is deliberately **ASM-free** — ASM exists only on the transform side.

## 1. From jar in to jar out — `compile/jar`

Jar processing is a single fused traversal in [`compile/jar/`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/):

- [`JarStep`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/JarStep.java) is the per-entry visitor contract:
  `prepareFused` once before the walk, `visitEntry` per entry, `endFused` once after, over a shared
  [`StepContext`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/StepContext.java) (flags, downgrader, a `data` map for
  cross-step state, the sink).
- [`FusedJarRun`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/FusedJarRun.java) prepares every step once, walks all
  input roots once via `AsyncUtils.visitPathsAsync`, applies steps in declaration order per entry, calls `endFused`,
  then flushes the [`JarEntrySink`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/JarEntrySink.java) — which sorts
  every path and pins timestamps, so identical inputs produce byte-identical outputs.
- `PathDowngrader` builds the exact chain:

  ```
  EmbeddedRulesStep → SignatureStripStep → ManifestStep
      → MultiReleaseRouteStep → ClassDowngradeStep | PipelineJarStep
  ```

  [`EmbeddedRulesStep`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/EmbeddedRulesStep.java) scans
  `META-INF/jvmdg/*.mf` in `prepareFused` — before any class is seen — applying rules sandboxed to the jar's own
  packages (§8). [`SignatureStripStep`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/SignatureStripStep.java) drops
  jarsigner files (`*.SF/RSA/DSA/EC`) whose digests the transform invalidates, flagging `StepContext.data` so
  [`ManifestStep`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/ManifestStep.java) strips now-dangling `Sealed:`
  attributes. [`MultiReleaseRouteStep`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/MultiReleaseRouteStep.java)
  decides what `META-INF/versions/<n>/` input entries become — dropped, passed through under `multiReleaseOriginal`, or
  re-anchored as primary candidates under `downgradeFromMultiReleases` (highest versioned copy of a name wins).
- The terminal step is one of:
  - [`ClassDowngradeStep`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/ClassDowngradeStep.java) — per-entry: each
    class goes through `ClassDowngrader.downgrade` and every produced artifact (finals, variants, generated helpers) is
    emitted; classes already at/below target pass through as lazy copies.
  - [`PipelineJarStep`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/PipelineJarStep.java) — whole-set: all classes
    run one `TransformPipeline` so the shade closure sees the entire post-transform set at once; captured variants are
    shaded in place and routed to `META-INF/versions/<n>/`.
- `ManifestStep.endFused` also stamps `JvmDowngrader-Version` / `JvmDowngrader-Downgrade-Target`
  ([`ManifestUtils`](/src/main/java/xyz/wagyourtail/jvmdg/compile/ManifestUtils.java)) and synthesizes `Multi-Release:
  true` when versioned output exists — the JVM silently ignores `META-INF/versions/` without it.

## 2. The pipeline engine — `pipeline/`

[`TransformPipeline`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/TransformPipeline.java) executes an ordered list of
[`Stage`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/Stage.java)s over ASM `ClassNode`s. The engine is
version-agnostic by design: a `PipelineRegistrar` hands it a stage list, so the same engine could drive a hypothetical
upgrade registrar as easily as the downgrade one.

- A `Stage` has a `name`, `ClassTransform`s, an optional `precondition`, an optional `filter`, `captureLabels`, and
  `onEnter`/`onExit` hooks.
- Each stage runs over a fresh [`TransformWorklist`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/TransformWorklist.java)
  — a `TreeMap<String, ClassNode>` keyed by internal name, so traversal and emitted-class draining are deterministic
  and a same-named emission replaces. Classes a transform emits go into `ctx.extras` and continue through the
  *remaining* transforms and stages; `snapshot()` gives a stable name-ordered view, so extras emitted during an extras
  pass aren't re-seen in that same pass.
- [`StageHook`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/StageHook.java) `onEnter` runs once per stage after the
  worklist is bound but before captures and the class loop — hooks can emit classes into the remaining pipeline, which
  is how barriers like the shade closure inject generated output. `onExit` runs once after the loop.
- `precondition` vs `filter` is a real distinction: a **precondition** throws when a class must not enter the stage at
  all (single-version stages; its failure is deliberately not wrapped in `TransformException`), while a **filter** lets
  non-matching classes pass through untransformed — still reaching `onExit` and later stages — which is how the
  mixed-version union chain routes each class to only its own boundary stages.
- `capture(label)` deep-copies each class at stage *entry* into `ctx.variants` (`Map<Object, List<ClassNode>>`).
  Integer labels mean "capture only classes at this class-file version"; copies clone indy bootstrap-arg arrays
  explicitly because later transforms mutate them in place. Variants are the raw material of multi-release output.
- [`StageScoped`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/StageScoped.java) artifacts — the shared method-analysis
  store is the big one — are cleared at every stage boundary and after the run, so analysis computed against one
  stage's bytecode can't leak into the next stage's mutated methods.
- [`TransformContext`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/TransformContext.java) carries per-run state:
  `index` (a [`ClasspathIndex`](/src/main/java/xyz/wagyourtail/jvmdg/asm/index/ClasspathIndex.java) over the
  downgrader's classpath), `extras`, `variants`, `resources` (non-class artifacts transforms emit), `diagnostics` (a
  [`DiagnosticSink`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/DiagnosticSink.java) — scoped, deduped, sorted on
  drain), and a free-form `data` map where registrars park private state (`ShadeContext`, `DowngradeContext`).
- [`TransformException`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/TransformException.java) preserves stage name,
  transform instance, and class name alongside the cause, so a failure deep in the chain reports *where* it happened.
- Adjacent transforms can fuse: [`FusibleTransform`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/FusibleTransform.java)
  declares mergeability, [`FusedTransform`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/FusedTransform.java) applies
  delegates in order inside one class walk — fusion merges traversal only, never resolution state. `flags.debugNoFuse`
  runs the same list sequentially; the two paths are behaviorally identical by construction.
- [`ExtrasApplyTransform`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/ExtrasApplyTransform.java) applies its delegate
  to the primary class plus a stable snapshot of the current extras, preserving legacy `stubWithExtras` semantics.

## 3. The version chain — `version/`

A [`VersionProvider`](/src/main/java/xyz/wagyourtail/jvmdg/version/VersionProvider.java) owns one class-file-version
boundary: its `inputVersion`/`outputVersion` pair, the stub registrations that boundary introduces, and the fixups
specific to that language/JDK delta. [`AllVersionDowngrader`](/java-api/src/main/java/xyz/wagyourtail/jvmdg/providers/AllVersionDowngrader.java)
builds the chain in the api jar.

[`DowngradeRegistrar`](/src/main/java/xyz/wagyourtail/jvmdg/version/DowngradeRegistrar.java) turns providers into
stages:

- `stages(Set<Integer> inputVersions)` emits one stage per boundary a present input version can reach, ordered by
  *descending* input version, so a class traverses newest→oldest and exits at target. A single-version input gets a
  `precondition` on `provider.inputVersion`; a mixed set unions the boundaries and each stage gets a `filter` on the
  class's major.
- A standard boundary decomposes into:

  ```
  ProviderInitTransform → StubClassesTransform → StubMethodsTransform
      → InsertAbstractsTransform → OtherTransformsTransform → FlushWarningsTransform
  ```

  `StubMethods`, `InsertAbstracts`, and `OtherTransforms` run wrapped in `ExtrasApplyTransform`; `FlushWarnings` prints
  the per-class warning set, and a stage `onExit` sweep (`DowngradeContext.flushAll`) covers classes dropped mid-stage.
  A provider overriding the wholesale `VersionProvider.downgrade` instead gets a
  [`ProviderStep`](/src/main/java/xyz/wagyourtail/jvmdg/version/ProviderStep.java) — the legacy all-in-one boundary
  call, kept as the oracle the decomposed path reproduces.
- All version comparisons go through `Utils.majorClassVersion` (`version & 0xFFFF`): `ClassNode.version` packs
  `minor<<16 | major`, and javac `--enable-preview` sets minor to `0xFFFF`. Preview inputs are rejected unless
  `flags.allowPreview`, then routed by major; `StubCondition.PREVIEW` entries gate stubs to preview contexts.

### Authored version vs. current `node.version`

`ProviderInitTransform` stamps the class with
[`@JvmDowngraded(from,to)`](/src/shared/java/xyz/wagyourtail/jvmdg/stub/all/JvmDowngraded.java) exactly once — `from` =
the classfile major the class was authored at, `to` = the downgrade target — and *then* rewrites `node.version` to the
provider's `outputVersion`. After the first boundary, `node.version` is a mutable output marker, not the authored
version. Emission sites needing the authored version read `provider.inputVersion`: the gained dispatchers' trailing
`ceiling` argument and `downgradeVersion` stub handlers consume it that way. The runtime reads the same contract back
off `@JvmDowngraded` — the reflection table applies a row when `since ∈ (to, from]`, and untransformed classes carry no
annotation at all.

### Contributor SPIs

- [`TransformContributor`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/TransformContributor.java) (`META-INF/services`,
  class-name sorted): appends `Stage`s at `BEFORE_VERSION_CHAIN`/`AFTER_VERSION_CHAIN` while the registrar builds the
  chain, and `POST_EMIT` after the last stage (where `PipelineJarStep` hooks post-shade work). `NoIndyContributor` is
  the stock example.
- [`ReflectionContributor`](/src/main/java/xyz/wagyourtail/jvmdg/version/ReflectionContributor.java): owns the
  reflection-stub surface — `java.lang.Class`, `MethodHandles$Lookup`, `java.lang.reflect.*` member registrations — and
  runs once per provider init. The api jar's `CoreReflectionContributor` registers the stock set via `META-INF/services`,
  so third-party reflection layers can add rows without touching providers; a row's version window lives in
  `reflection.idx`'s `since` column, not in which provider asked.

## 4. The stub model — `version/map` + `version/idx`

Stubs are *registered behavior*: declarations saying "for this JDK member (or type, or ctor shape), use this
implementation at this boundary." Authoring details live in [Stubs.md](Stubs.md); this section covers the machinery.

### Registration

- [`StubScanner`](/src/main/java/xyz/wagyourtail/jvmdg/version/StubScanner.java) reflectively scans provider stub
  classes and dispatches each primary annotation to a `version/scan/` handler — `@Adapter` on classes (type remaps),
  `@Stub`/`@DefaultImpl`/`@Modify` on methods, the `@InitFactory`/`@InitRewrite`/`@InitArg`/`@InitPre`/`@InitPost`
  ctor-shape family, `@IndyDesugar` on bootstrap desugars. One annotation = one registration; a doubly-annotated member
  is an error. `@JEP` filters by JEP id; `@Coerce`, `@ReflectionReferences`, `@RequiresResource` annotate registrations.
- Registrations land in the provider's [`StubRegistry`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/StubRegistry.java):
  a per-owner store of `methodStub`, `fieldStub`, `ctor`, `memberModify` (null key = class-level wildcard), and
  `indyDesugars` maps, plus side data (`memberSince` from the build-time `ct.sym` pass, `memberKind` overrides) that
  `kindOf` uses to classify a site as `LANGUAGE`, `BEHAVIOR` (member exists natively at target but semantics differ),
  or `MISSING_API`. `flags.stubPolicy`/`flags.onMissingApi` — or scoped-rule equivalents — veto or error on kinds
  post-resolution (`MemberStubTransform.vetoed`), and jvmdg's own impl classes skip `BEHAVIOR` stubs so the
  interception layer can't delegate into itself.
- [`ClassStubEntry`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/ClassStubEntry.java) is the `@Adapter` record:
  `target`, `stubType`, `keepInterface`, the `itf` bit, baked `jvmdg$instanceof`/`jvmdg$checkcast` hooks, `jeps`,
  retro-supertype `mode` (`SUPERTYPE` vs `OWN_INTERFACE`), `implementedBy` membership, `wrap` factory, and the
  signature-polymorphic `jvmdg$bootstrap` hook + name set. Everything is baked at registration — the transform path
  never reflect-loads stub classes (runtime loaders would see pre-downgrade api bytes or miss test fixtures entirely).
- [`StubEntry`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/StubEntry.java) is the member record: the `MethodRef`
  handler plus redirect modifiers `downgradeVersion`, `callerClass`, `condition` (`ALWAYS`, `REQUIRES_RUNTIME`,
  `PREVIEW`), `abstractDefault`, `noSpecial`, `excludeChild`.

### Resolution

[`MemberResolver`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/MemberResolver.java) and
[`TypeRemap`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/TypeRemap.java) are the narrow oracles emission consumes;
`ClassMapping` implements both as the per-owner node of a lazy resolution graph. Resolution walks the *source-version*
supertype hierarchy: a supertype's stub applies to subtype callsites unless `excludeChild` vetoes it; the receiver's
own native member check runs first so an existing member is never stubbed away; ctor entries resolve owner-exact (a
`new Sub()` never inherits `Super`'s rewrite). `TypeRemap` additionally exposes `gainedFor` — the generated dispatch
owner for a gained-supertype interface — as pure emission data.

Hierarchy and member-existence queries go through [`ClasspathIndex`](/src/main/java/xyz/wagyourtail/jvmdg/asm/index/ClasspathIndex.java)
— a memoized, thread-safe view of the downgrader's classpath; input-class originals are deliberately *not* held there —
via per-call `NamespaceView`s that remap through the provider chain before resolving. `WorkingClasses` gives O(1) name
lookup over the extras set.

### The index: `stubs.idx`

The reflective scan is the semantic source of truth, but it is slow and needs the api classes loadable.
[`StubIndex`](/src/main/java/xyz/wagyourtail/jvmdg/version/idx/StubIndex.java) is a deterministic binary codec —
`META-INF/jvmdg/stubs.idx`, format version 7, stamped with the producing jvmdg version — serializing the post-scan
registries. [`StubIndexWriter`](/src/main/java/xyz/wagyourtail/jvmdg/version/idx/StubIndexWriter.java) runs at api-jar
build time (`genStubIndex`), enriched by the `genCtSym` JDK-surface index: it stamps `memberSince`, derives
`implementedBy` membership and `excludeChild` unions, and flattens derived tables into runtime resources:

- `META-INF/jvmdg/retrotypes` — retro-supertype membership rows
- `META-INF/jvmdg/wraps` — OWN_INTERFACE adaptation rows
- `META-INF/jvmdg/gained` + `gained-manual` — gained-supertype rows (derived + hand-maintained)
- `META-INF/jvmdg/gained-calls` + `gained-calls-manual` — member-level gained-call rows
- `META-INF/jvmdg/excludes` — derived `excludeChild` rows for the scan path
- `xyz/wagyourtail/jvmdg/stub/reflection.idx` — the reflection table, a package-relative resource next to
  `ReflectionTable` (carried for shading by `@RequiresResource(ReflectionTable.RESOURCE)`)

Providers idx-load at `ensureInit`; `jvmdg.debug.noStubIdx` forces the scan. Two adapters keep the paths
indistinguishable: [`DerivedExclusions`](/src/main/java/xyz/wagyourtail/jvmdg/version/idx/DerivedExclusions.java) unions
the excludes resource onto scan-built entries (idx records already carry baked exclusions), and
[`DerivedGained`](/src/main/java/xyz/wagyourtail/jvmdg/version/idx/DerivedGained.java) load-filters gained rows per
boundary into `vp.gained` — an interface counts only when real at the final target (`imin ≤ flags.classVersion`) and
the member binding crosses *this* boundary (`output < since ≤ input`). Because scan and idx must emit identically,
**emission is never gated on idx-derived data alone**.

`vp.gained` also drives two structural effects: `stubClasses` injects a gained interface into a user class whose
supertype closure gained it (the resolver sees the source-version hierarchy where it already holds), and
`insertAbstractMethods` emits public bridges for gained-call members the class doesn't serve natively —
invokestatic-ing the impl handler directly, never the dispatcher (a bridge to the dispatcher would mark the class as
serving the member and recurse).

### Generated api classes

[`version/gen/`](/src/main/java/xyz/wagyourtail/jvmdg/version/gen/) holds the api-jar build-time codegen (`ApiGenerator`
+ `GenContext`), run inside `genStubIndex` over the same post-scan registries: `MarkerGenerator` (`<stub>$Marker`
interfaces), `RetroDispatchGenerator` (`<stub>$jvmdg$retro` dispatchers), `GainedDispatchGenerator` (`gen/gained/<I>` +
`$Wrap`), `MemberForwarderGenerator` (`<adapter>$jvmdg$fwd` hosts for `AdapterForwarders`' post-scan registrations).
Generated classes emit at `V1_8` (the lowest runtime floor), a hand-declared class always wins (`ctx.declared`), and
they are *ordinary api classes*: indexed, shaded, pruned, and downgraded like authored ones.
`GenContext.flattenedWraps` computes the single most-specific-first wrap ordering both the `wraps` table and the
`$jvmdg$retro` dispatchers consume, so table and bytecode can't drift.

## 5. Dispatch machinery

This is the heart of behavioral parity: what actually happens at a callsite after the rewrite.

### 5.1 Member stubs — invokedynamic + a per-callsite PIC

`INVOKEVIRTUAL`/`INVOKEINTERFACE` sites resolved to an instance stub emit an `invokedynamic` bootstrapped by
[`stub/runtime/DispatchRuntime`](/src/shared/java/xyz/wagyourtail/jvmdg/stub/runtime/DispatchRuntime.java);
[`DispatchSites`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/DispatchSites.java) owns the transform-side contract.
Bootstrap args carry the member `MethodType` plus the impl as a `Class` constant + `"name desc"` string — resolved
lazily because the api jar's declared signature differs per packaging (raw shaded vs. downgraded) and an eager `Handle`
would poison `<clinit>` under `--noIndy`.

At runtime the callsite's dispatch chain is a `ClassValue`-backed polymorphic inline cache: `findVirtual` resolves the
receiver's *own* declared-or-inherited member exactly like authored virtual dispatch, so **user overrides win**; the
registered self-first impl handle is only the fallback. No marker interface is injected — `getInterfaces()` shows only
what the source declared. Varargs impls adapt via `MethodHandles.explicitCastArguments`; access failures throw rather
than silently fall back. The same resolution is exposed reflectively as `dispatchStub` — the `D:`-prefixed site
`gained-calls` fallback rows name, so the fallback path keeps the override-wins contract.

Three intentional asymmetries:

- `INVOKESPECIAL` sites keep the impl — super-constructor/`super.m()` semantics don't route through virtual dispatch.
- A member-stub *miss* on an `@Adapter(signaturePolymorphic=…)` owner (VarHandle's access modes) emits a second
  contract, `DispatchSites.polymorphicInsn`: site name mangled with the stub type, bootstrap = the adapter's
  `jvmdg$bootstrap` hook, no bsmArgs — the hook derives the impl signature from the callsite name+desc at link time.
- Each emitted site stamps `@ReflectionReferences` on its containing method (`MemberStubTransform.addReflectionRef`,
  desc remapped into stand-in vocabulary) so the shade closure keeps the reflectively-reached impl.

### 5.2 Retro supertypes — `$jvmdg$retro` + `$Marker` + `RetroTypes`

A *retro supertype* is a stub interface/adapter injected as a supertype of a JDK class whose runtime class does not
actually implement it. `StructuralRemapTransform` rewrites `instanceof`, `checkcast`, array forms, and class queries
against it into a single `invokestatic` on the generated `<stubType>$jvmdg$retro` dispatcher (from
[`RetroDispatchGenerator`](/src/main/java/xyz/wagyourtail/jvmdg/version/gen/RetroDispatchGenerator.java)).

The dispatcher carries real bytecode for `wrap`, construction, `isInstance`, `checkcast`, array forms,
`isAssignableFrom`: the wrap path is adapter `instanceof` → an ordered `instanceof`/`new` chain over registered impls
(most-specific-first, the `flattenedWraps` ordering) under `catch (LinkageError)`, tail-calling
[`RetroTypes.wrapImpl`](/src/shared/java/xyz/wagyourtail/jvmdg/stub/runtime/RetroTypes.java); query paths combine
adapter/marker/impl-member `instanceof`s with table tails. Real bytecode lets C2 inline and scalar-replace what
reflection couldn't; impl-less OWN_INTERFACE adapters still get a fallback-only dispatcher. `$Marker` interfaces
(generated when the adapter doesn't declare one) give `SUPERTYPE`-mode membership something concrete to test: the
transform injects them into downgraded classes whose `implements` clause named the retro type.

`RetroTypes` is the ASM-free runtime table behind it: lazily merges every classpath's `retrotypes`/`wraps`/`gained*`
resources, answers membership over superclass/interface closure with `ClassValue` caches, and stays the fallback for
large JDK member sets, externally-registered rows (`register`, `gainedRegister`, `gainedCallRegister`), and missing
runtime types. Lookup is name-keyed and scoped to the caller's loader context, so two prefixed shaded jars can't steal
each other's impls.

### 5.3 Gained supertypes — `gen/gained/<I>` helpers + indy

A *gained supertype* is the mirror problem: a JDK class that *acquired* an interface after the authored version —
`StringBuffer implements Comparable` at class-file 55 is canonical. On the target JVM the class doesn't implement it,
so:

- **Class-query sites** (`instanceof`, `checkcast`, array checks, `isAssignableFrom` queries) can't verify against a
  missing type. `StructuralRemapTransform` routes them via `TypeRemap.gainedFor` to the generated
  `xyz/wagyourtail/jvmdg/gen/gained/<mangled-I>` helper's structural methods (`jvmdg$isInstance`, `jvmdg$cast`,
  `jvmdg$isAssignableFrom`, array forms), each carrying a **trailing `int` ceiling — the calling class's authored
  version** — so membership is answered against the version the source was written for.
- **Member-call sites** on the gained interface emit `invokedynamic` bootstrapped by `stub/runtime/GainedRuntime` (bsmArgs:
  source owner, `"name desc"` member key, ceiling; the site desc erases refs to `Object` and re-narrows with a trailing
  `checkcast`). Routing through indy is a measured perf decision: a `catch (LinkageError)`-per-call dispatcher cost
  roughly **200–600×** the PIC path on targets missing the interface.
- Each dispatcher has a **`$Wrap` sibling** implementing the real interface — the verifier-legal way to hand a member
  object to a consumer demanding the real type; its members route through `RetroTypes.gainedCall`.
- **`aastore` into gained-interface arrays** wraps member values before storing, then leaves a *native* `AASTORE` tail
  so a genuinely wrong store still throws the honest `ArrayStoreException`; reprocessing dedupes the inserted routing.
- [`GainedSites`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/GainedSites.java) owns the site contracts;
  `isDispatchMachinery` exempts the machinery itself (`stub/`, `gen/`, `$jvmdg$`/`$Ov$` owners) — indy inside a
  bootstrap would self-recurse.
- `RetroTypes.gainedCall` resolves *native declarer first, registered stub rows second* — receiver-name resolution
  includes `MEMBER_OF` reverse mappings so stand-ins match source-member gained rows — and an unresolvable row throws a
  real `AbstractMethodError`.

### 5.4 Reflection stubs — `ReflectionTable`

`Class.forName`, `getMethod`, `MethodHandle` lookups bypass bytecode rerouting, so reflective access gets a table-driven
answer: [`ReflectionTable`](/src/shared/java/xyz/wagyourtail/jvmdg/stub/reflection/ReflectionTable.java) (ASM-free, lazy resource
load) maps source JDK types to api stand-ins with `since` versions; member rows carry callsite + impl owner/name/desc,
instance/static shape, and JDK modifiers. A row applies only when `since ∈ (target, authored]` — read off the
callsite's `@JvmDowngraded`, so an untransformed class still sees the honest `ClassNotFoundException` /
`NoSuchMethodException` the newer JDK would throw; lookup resolves through the *caller's* classloader.

Because reflection-only impl references leave no bytecode edge, stub impls (and generated dispatchers) declare them with
`@ReflectionReferences` `@Ref` rows — the reachability input shading's closure follows (§10).

### 5.5 `OptionalDependency` — the api jar stays ASM-free

A few stub impls genuinely need runtime transformation (`Lookup.defineClass`/`defineHiddenClass` receiving too-new
bytes). They probe [`stub/runtime/OptionalDependency`](/src/shared/java/xyz/wagyourtail/jvmdg/stub/runtime/OptionalDependency.java) for a
[`RuntimeDowngrader`](/src/shared/java/xyz/wagyourtail/jvmdg/stub/runtime/RuntimeDowngrader.java) via `ServiceLoader` — the core
jar's `CoreRuntimeDowngrader` implements it — so api classes never carry a constant-pool edge to core types. Absent the
core, impls fall back to as-is behavior, correct when input bytes are already at or below target.

## 6. Method analysis — `MethodAnalyses`

Some rewrites need dataflow, not pattern matching: which `Class` literal feeds this `isInstance` call, which array type
this `aastore` stores into. [`MethodAnalyses`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/MethodAnalyses.java) is
the run-scoped channel: one fused analysis per `MethodNode`, memoized by identity in `TransformContext.data`,
`StageScoped` so it clears at every boundary (frames always describe the method as the current stage found it).

`StructuralRemapTransform.MethodAnalysis.frames()` computes fused producer+type frames in two passes: a *trusting* pass
reusing the method's declared stack-map frames, falling back to a full `Analyzer` fixpoint with a `FusedInterpreter` for
pre-50 inputs, frame-skipping parses, or JSR/RET methods. Unverifiable methods yield null frames and their insns are
left native rather than guessed. Consumers today: `remapClassCalls` (routing `ldc`'d-retro `Class.isInstance`/`cast`/
`isAssignableFrom` receivers through the same dispatchers) and the gained `aastore` routing — with concrete
value/array-type provenance kept for future value-class work.

## 7. Multi-release

Multi-release is a *variant* mechanism end to end:

- **Input**: `MultiReleaseRouteStep` routes `META-INF/versions/<n>/` entries per
  `downgradeFromMultiReleases`/`multiReleaseOriginal` (§1).
- **Output**: `Stage.capture(version)` snapshots classes at stage entry into `ctx.variants`; `PipelineJarStep` routes
  each to `META-INF/versions/<major>/`, deduplicating byte-identical copies past the 8-byte class header.
  `ManifestStep` then ensures `Multi-Release: true`.
- The authored-vs-output distinction matters: the variant ceiling is the *authored* version at capture time, which is
  why captures happen at stage entry — before `ProviderInit` rewrites `node.version`.
- Under shading, `ShadeContext.positionOf` treats the versioned slot as the position dimension — captured variants are
  shaded in place at the closure barrier, since the name-keyed worklist can't hold two same-named versions of a class.

## 8. Embedded rules

A jar can carry `META-INF/jvmdg/*.mf` rule files — scoped stub inclusions/exclusions, JEP filters, policy overrides.
`EmbeddedRulesStep.prepareFused` reads them before the entry walk and applies them via `JvmdgConfig.applyEmbedded`,
sandboxed to the jar's own packages so a dependency can't restyle the whole build. Rule files (and an empty
`META-INF/jvmdg/` dir) are stripped by default; `Keep-Embedded-Rules` preserves them (a downgraded library re-exporting
its rules) and `Ignore-Embedded-Rules` leaves them entirely untouched.

## 9. `--noIndy`

`flags.noIndy` appends java-api's [`NoIndyContributor`](/java-api/src/main/java/xyz/wagyourtail/jvmdg/providers/NoIndyContributor.java)
stage `downgrade:noIndy` at `AFTER_VERSION_CHAIN` — after boundary desugars, before shade stages — running
[`NoIndyTransform`](/java-api/src/main/java/xyz/wagyourtail/jvmdg/providers/NoIndyTransform.java):

- Registered `@IndyDesugar` handlers union across providers newest→oldest — normalizers (e.g. the j23 `typeSwitch` shape
  fix) run ahead of older real desugars — and each site re-offers itself until it is no longer indy.
- Unregistered bootstraps fall back to generic `jvmdg$indy$` lowering: a static `CallSite` field initialized once in
  `<clinit>` plus a synthetic helper invoking `getTarget().invokeExact` (the same shape the j7→6 boundary uses), with a
  deduped `DiagnosticSink` warning.
- `ldc` of a `ConstantDynamic` becomes a `jvmdg$condy$N` static field resolved once in `<clinit>`.
- jvmdg's own dispatch indys desugar through carriers like `DispatchRuntimeDesugar`/`GainedRuntimeDesugar` —
  transform-time classes never loaded at runtime, since ASM types can't exist on target JVMs;
  `GainedSites.isDispatchMachinery` keeps machinery out of the sweep, with gained sites lowering to the reflective
  `RetroTypes.gainedCall` tail.
- Hard post-condition: output contains **zero** `InvokeDynamicInsnNode` / `ConstantDynamic`.

## 10. Shading — `compile/shade` + `ApiShader`

`ApiShader` answers "which api classes does this input actually need, and how do they get renamed?" by reference
closure:

- [`ReferenceIndex`](/src/main/java/xyz/wagyourtail/jvmdg/compile/shade/ReferenceIndex.java) is a slot-aware reference
  table; [`IndexScanner`](/src/main/java/xyz/wagyourtail/jvmdg/compile/shade/IndexScanner.java) feeds it classes at
  their multi-release slot. [`MemberRefExtractor`](/src/main/java/xyz/wagyourtail/jvmdg/compile/shade/MemberRefExtractor.java)
  pulls *every* reference shape out of a `ClassNode` — supertypes, signatures, annotations incl. nested values,
  inner/outer/nest/permitted metadata, record components, member descs/exceptions/try-catch, all insn kinds, and the
  indy + condy bootstrap graphs — with `MemberScanner` hooks adding semantic edges: `@RequiresResource` paths attach to
  the annotated element, `@ReflectionReferences` `@Ref`s record members reflection reaches (the only reason
  reflectively-used impls survive pruning).
- Lookup is floor-versioned: a query at version *v* sees the newest api member introduced at ≤ *v*, so the closure can't
  pull in api the target can't run.
- In-pipeline, [`ShadeRegistrar`](/src/main/java/xyz/wagyourtail/jvmdg/compile/shade/ShadeRegistrar.java) appends
  `shade:scan` (the input index — anchored *after* the version chain because the refs that matter are the ones the
  downgrade introduced), the `shade:closure` barrier
  ([`ShadeClosureBarrier`](/src/main/java/xyz/wagyourtail/jvmdg/compile/shade/ShadeClosureBarrier.java): per-slot
  closure, inline candidates, prefix remappers, emitted api classes, resource copies — over primaries *and* captured
  variants shaded in place), then `shade:inline`, `shade:prefix`, `shade:module-info`.
- The barrier rewrites more than bytecode: member names/descriptors, gained-call site prefixes, `wraps` rows, and
  `reflection.idx` rows all get the remap. Reflection rows prune per `flags.shadeReflection` (default `USED_OWNERS`:
  keep impls reachable from reflection entry points the input calls, plus `shadeKeepReflection` pins).
- `META-INF/services/*` files are remapped and merged (`mapServiceEntry` / `mergeServices`) so `ServiceLoader` keeps
  working under the prefix.
- Inline candidates — api members small/stable enough to splice into the caller — move under `flags.shadeInlining`;
  everything else is emitted as a prefixed api class at its own slot. Generated dispatchers and markers are ordinary
  api classes here: indexed, prefixed, pruned, downgraded like authored ones.

## 11. Self-hosting

JvmDowngrader downgrades *itself* before shipping (`gradle.properties`: `stubToVersion=27`, `mainVersion=8`):

```
compileJava/compileSharedJava (j27 toolchain → v71 classes)
  → rawCoreJar / rawSharedJar
  → selfDowngradeCore / selfDowngradeShared
      (this build's own ZipDowngrader, on a j27 launcher,
       against java-api's dedicated selfHostApiJar)
  → build/selfhost/core-downgraded-8.jar, shared-downgraded-8.jar
  → jar / shadowJar pack those + the v52-downgraded api (downgradeJar8)
```

The dedicated `selfHostApiJar` — providers + stubs + idx wrapping *raw* shared classes — exists solely to break the
`apiJar → selfDowngrade → apiJar` task cycle; only the j27 launcher ever sees it. `dropRawToolchainClasses` removes any
class still > v52 leaking in from other source sets. `compileMainV52`/`compileSharedV52` javac the same sources under a
Java 8 classpath as a language/API-floor gate for pre-27 consumers (java-api, gradle-plugin, javac-plugin) — never
shipped. `selfHostSmokeTest` runs the fat jar's `Main --version` on a j8 launcher. Consequence for contributors: **Java 8
language/APIs are the norm in `src/main`/`src/shared`** — anything newer needs stub coverage at the 8 boundary, because
self-downgrade only covers what the stub chain rewrites.

## 12. Runtime delivery — `classloader/` + `runtime/`

- [`DowngradingClassLoader`](/src/main/java/xyz/wagyourtail/jvmdg/classloader/DowngradingClassLoader.java) loads
  api/provider/generated packages child-first where necessary so raw too-new api/provider classes are transformed by a
  lazily-built current-version `ClassDowngrader`; the `downgrade` products — primary class plus generated extras — are
  defined under class-loading locks, and transform failures surface as `IllegalClassFormatException`.
- [`ResourceClassLoader`](/src/main/java/xyz/wagyourtail/jvmdg/classloader/ResourceClassLoader.java) underlies it: a
  multi-release-aware resource loader probing `META-INF/versions/<n>/` prefixes newest→oldest down to the running JVM's
  max class version, over a `CopyOnWriteArrayList` of delegates (added while parallel tasks enumerate them).
- [`ClassDowngradingAgent`](/src/main/java/xyz/wagyourtail/jvmdg/runtime/ClassDowngradingAgent.java) is the `-javaagent`
  transformer: it downgrades class bytes in `transform`, defines generated extras, handles retransform races, and — a
  hard rule — converts failures to `IllegalClassFormatException` rather than ever calling `System.exit` inside a host
  JVM.
- [`Bootstrap`](/src/main/java/xyz/wagyourtail/jvmdg/runtime/Bootstrap.java) has two modes: standalone (build a
  `DowngradingClassLoader` over the given classpath, set it as the TCCL to match `java -cp` semantics, reflectively
  invoke the real main) and `premain`/`agentmain` (downgrade each java-api zip to a sha-keyed cache jar, append it and
  self to the bootstrap search, register the agent, and retransform `java.security.CodeSource` so signed-class
  verification survives).

## 13. Testing and verification

- `./gradlew :testing:test` runs the full matrix: unit tests plus end-to-end downgrades across every JDK from
  `testTargetVersion` to `stubToVersion`. The [`testing/`](/testing/) corpus layout — feature corpus with line-diffed
  `RESULT:`/`EXC:` output, `stubtests*` parity corpus, `multiVersion` fixtures — is documented in
  [AGENTS.md](/AGENTS.md); `build/reports/stubtests/` carries parity reports and `stubparity-known-diffs.txt` pins
  accepted diffs in both directions.
- Coverage machinery — how unstubbed surfaces are found and reported — lives in [Coverage.md](Coverage.md), which
  also documents the behavioral-parity caveats of the stub surface.
- Unit-level tests to know when changing machinery: `TestOtherTransforms` and `TestNoIndy` exercise boundary fixups and
  the no-indy sweep with crafted `ClassNode`s (no JVMs); the idx/scan parity oracle guards load-path equivalence; fused
  vs. `debugNoFuse` paths are identical by construction.

## Further reading

- [Stubs.md](Stubs.md) — authoring stubs (`@Stub`, `@Adapter`, `@Modify`, init hooks, coercion)
- [Configuration.md](Configuration.md) — the full flag surface
- [Coverage.md](Coverage.md) — coverage/unstubbed-surface machinery + behavioral-parity caveats
- [Contributing.md](Contributing.md) — contribution workflow
- [AGENTS.md](/AGENTS.md) — the canonical, denser architecture map + build/test corpus notes
