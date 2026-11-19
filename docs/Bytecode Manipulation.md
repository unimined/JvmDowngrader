# Bytecode Manipulation

What JvmDowngrader does to class bytes: the transform model, the emission
rules every transform obeys, and the per-version structural edits. This is
the internals doc — not the authoring guide and not the machine overview.

## Where the rest lives

* [Architecture.md](Architecture.md) — the 3.0 machine: pipeline engine,
  stage list, worklist traversal, and the dispatch machinery
  (`DispatchRuntime`, `$jvmdg$retro`/`gained` dispatchers, reflection stubs).
* [Stubs.md](Stubs.md) — authoring guide: `@Stub`/`@Adapter`/`@Modify`/
  `@IndyDesugar`/ctor-rewrite registrations and their annotation knobs.
* [Configuration.md](Configuration.md) — every flag/sysprop/config key.
* [Coverage.md](Coverage.md) — stub coverage state and the coverage report.
* [AGENTS.md](../AGENTS.md) — the full agent-facing repo map.

## The model

All transforms operate on ASM tree-api `ClassNode`s — bytes are parsed once
into a name-keyed worklist, mutated in place (or swapped), and serialized
once at the end. A transform may also append helper classes to the run's
extra set; `ExtrasApplyTransform` rejoins them to the worklist so downstream
stages see them. Hierarchy queries go through the memoized
[`ClasspathIndex`](/src/main/java/xyz/wagyourtail/jvmdg/asm/index/ClasspathIndex.java)
/`ClassMapping` resolver graph — never `Class.forName`.

Registered content is scanned once per provider by
[`StubScanner`](/src/main/java/xyz/wagyourtail/jvmdg/version/StubScanner.java)
into [`StubRegistry`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/StubRegistry.java)
(frozen at stage entry), or idx-loaded from `META-INF/jvmdg/stubs.idx` when
the api jar ships one. The per-boundary stage decomposition
(`ProviderInit → StubClasses → StubMethods → InsertAbstracts →
OtherTransforms → FlushWarnings`) is Architecture.md's business; what each
registration kind does to the bytes is below.

## Emission rules

These hold across every transform, hand-written or generated:

* **No `COMPUTE_FRAMES`, ever.** Writers run with `ClassWriter.COMPUTE_MAXS`
  only (`ClassDowngrader`, `PipelineJarStep`). New insns either leave
  existing frames untouched or hand-author `FrameNode`s at the insertion
  point — the maintained frames are what later analysis stages (e.g.
  `remapClassCalls`, fusion preflights) consume.
* **Deterministic names.** Every synthesized class/member name is a pure
  function of (class, site): `jvmdg$indy$<name>$<n>`,
  `jvmdg$handle$<name>$<n>`, `jvmdg$methodtype$<n>`,
  `jvmdowngrader$nest$<owner>$<member>`, `<Owner>$jvmdg$StaticDefaults`,
  `jvmdowngrader$construct$<caller>$<n>`. No global counters, no
  traversal-order dependence; jar output is a sorted, timestamp-pinned
  write.
* **No reflection on stub classes from emitters.** Class loading on the
  transform path sees pre-downgrade api classes (or misses synthetic test
  fixtures entirely), so everything an emitter needs — the
  stub-is-interface `itf` bit, hook members, impl mappings — is baked into
  the registry at scan/idx-load time.
* **Preview classes.** `ClassNode.version` is `minor << 16 | major`;
  `javac --enable-preview` sets minor `0xFFFF`. All version compares go
  through `Utils.majorClassVersion` (major-masked). Preview input is a hard
  error unless `--allowPreview`, in which case it routes through its
  major's provider chain and `StubCondition.PREVIEW` registrations
  activate.
* **Verifier-gated surgery.** Constructor rewrites prove the `NEW`/`DUP`
  allocation shape (and `this()`/`super()` delegation) before splicing;
  unproven shapes are left unchanged with a warning rather than emitting
  broken bytecode.

## Transform taxonomy

Each registration kind maps to a distinct bytecode effect:

| Kind | Effect on input bytes |
|---|---|
| member `@Stub` | callsite or field access rewritten — `invokestatic` to the impl, or an `invokedynamic` dispatch site |
| class `@Stub` / `@Adapter` | every occurrence of the JDK type remapped to the stand-in type (descriptors, signatures, insns, annotations, frames) |
| `@Modify` | handler invoked at the matched insn index; splices arbitrary replacement insns |
| `@IndyDesugar` | rewrites (or removes) an indy site whose bootstrap is versioned out |
| ctor family (`@InitRewrite`/`@InitFactory`/…) | proven `NEW`/`DUP`+`<init>` sequences retargeted or replaced by a factory `invokestatic` |
| `otherTransforms` | per-boundary structural edits — the versioned list below |

### Member stubs → invokestatic / invokedynamic

How a resolved stub member is emitted depends on the callsite opcode:

* `INVOKESTATIC` / field access → straight `invokestatic` to the impl.
* `INVOKEVIRTUAL` / `INVOKEINTERFACE` → an `invokedynamic` bootstrapped by
  `xyz/wagyourtail/jvmdg/stub/runtime/DispatchRuntime.bootstrap`; bsmArgs carry the
  member `MethodType` + the impl as an owner `Class` + `"name desc"` string
  (resolved lazily at link, so no `Handle` constant poisons `<clinit>` under
  `--noIndy`), and a per-callsite `ClassValue` PIC resolves the receiver's
  own member at link time so user overrides win.
  `@Adapter(signaturePolymorphic=…)` owners (VarHandle's access modes) get
  the second contract instead — `DispatchSites.polymorphicInsn` — where the
  site name carries the erased signature and the adapter's own
  `jvmdg$bootstrap` hook derives the impl.
* `INVOKESPECIAL` → keeps an `invokestatic` to the impl (no dispatch —
  `super`/private calls have a fixed target).
* `abstractDefault` / `@DefaultImpl` members → no callsite rewrite at all:
  a real `public` method is inserted into each implementor that lacks it
  (the `InsertAbstracts` stage), and only `invokespecial`
  (`Interface.super.m()`) routes to the stub impl.
* `X::new` method refs (`H_NEWINVOKESPECIAL` impl handles in indy bsmArgs)
  → `jvmdowngrader$construct$<caller>$<n>` static wrappers that replay the
  ctor rewrite or call the factory; other rewritten handle kinds get
  `jvmdowngrader$call$<caller>$<n>`.

### Class stubs / adapters → type remap + wrap dispatch

`StructuralRemapTransform` does the mechanical type-slot remap for
class-stubbed and adapted types. The sites a remap alone can't fix route
to generated dispatchers:

* `instanceof`/`checkcast`/class-query sites on stubbed types → a single
  `invokestatic` to the stub's generated `<stubType>$jvmdg$retro`
  dispatcher — `.wrap` for `OWN_INTERFACE` adaptation, `.cast`/
  `.isInstance`/array forms elsewhere.
* Gained supertypes (a JDK class that acquired an interface after the
  target) → `xyz/wagyourtail/jvmdg/gen/gained/<I>` structural helpers for
  type tests; member calls go through `invokedynamic` bootstrapped by
  `stub/runtime/GainedRuntime` with the calling class's authored version as a
  trailing ceiling arg.
* `ldc`'d retro `Class.isInstance`/`cast`/`isAssignableFrom` receivers are
  routed through the same dispatchers by the `remapClassCalls`
  `Analyzer`/`SourceInterpreter` pass.

### `@IndyDesugar` → indy site rewrite

When a callsite's *bootstrap* is itself versioned out, the registered
`@IndyDesugar` handler rewrites the site — usually to a synthesized
`jvmdg$…` `invokestatic`, sometimes to inline insns:

* j8 `LambdaMetafactory.metafactory`/`altMetafactory` → spins a real inner
  class implementing the SAM (`makeLambdaInnerClass`) and replaces the site.
* j9 `StringConcatFactory.makeConcat[WithConstants]` → splices an inline
  `StringBuilder` append chain.
* j16 `ObjectMethods.bootstrap` (record `equals`/`hashCode`/`toString`).
* j21 `SwitchBootstraps.typeSwitch`/`enumSwitch` → a generated private
  static method replaying the label semantics (`makeSwitchInternal`).
* j23 `SwitchBootstraps.typeSwitch` → *normalizer*, not a desugar: j23+
  javac emits the site with the labels' common supertype as selector and
  drops the callsite cast, so the handler rewrites `indy.desc` back to the
  pre-23 `(Ljava/lang/Object;I)I` shape the j21 handler consumes.
* `ldc <ConstantDynamic>` with a versioned bsm is re-emitted through the
  same machinery (`stubCondy`); typeSwitch *label* types are preserved
  (`stubBSMArgs`' `preserveLabels`) so the emitted switch can route stubbed
  labels through `RetroTypes` by name.

Under `--noIndy` the union of every provider's `indyDesugars` runs
newest→oldest until no `InvokeDynamicInsnNode`/`ConstantDynamic` remains —
normalizers like j23 `typeSwitch` run ahead of older real desugars — and
unregistered bsms fall back to the generic `jvmdg$indy$` callsite-field
lowering (the same shape as the j7→6 boundary below).

## Multi-release snapshots

`Stage.captureLabels` deep-copy the whole class set at stage entry into
`TransformPipeline.variants()`. `PipelineJarStep` then emits each captured
variant at `META-INF/versions/<slot>/` alongside the fully-downgraded
finals — that's the `--multiRelease` implementation. `MultiReleaseRouteStep`
does the inverse on input: `META-INF/versions/N/` classes are dropped unless
`downgradeFromMultiReleases` is set (re-anchored to their stripped path,
prefer-highest source per name) or `multiReleaseOriginal` keeps them in the
output (verbatim, or tagged at their slot for in-place shading).

## Per-version `otherTransforms`

The structural edits each provider applies, verified against
[`java-api/…/providers/`](/java-api/src/main/java/xyz/wagyourtail/jvmdg/providers/).
Boundaries not listed run no `otherTransforms` — they're pure
stub/registration boundaries.

* **25 → 24** — instance-`main` support (JEP 512 launch forms):
  `addMissingMains` synthesizes `public static void main(String[])` launchers
  for static/instance/no-arg main shapes; `renameInstanceMains` renames
  instance `main` to `jvmdg$instanceMain` and retargets non-static callsites.
* **23 → 22** — `typeSwitch` site normalization (via `@IndyDesugar`, above).
* **21 → 20** — `typeSwitch`/`enumSwitch` indy desugar (above).
* **17 → 16** — `unseal`: the `permittedSubclasses` attribute is moved to a
  runtime `@PermittedSubClasses` annotation and dropped from the class.
* **16 → 15** — `recordRemover`: `ACC_RECORD` stripped; `recordComponents`
  re-encoded as a `@RecordComponents` annotation.
* **15 → 14** — `fixHandleAccessNests`: private members reached through
  indy bsm/Handle bsmArgs by nestmates get
  `jvmdowngrader$handleNest$<owner>$<member>` accessor methods on the owner
  and the handles retargeted.
* **11 → 10** — `fixNests`: `nestHostClass`/`nestMembers` attributes moved
  to `@NestHost`/`@NestMembers` annotations, and private cross-nest-member
  field/method insns rewritten to `jvmdowngrader$nest$<owner>$[get$|set$]<name>`
  accessors (`<init>` is made package-private instead). `replaceCondy`:
  `ldc <ConstantDynamic>` → `invokedynamic` via
  `J_L_I_ConstantBootstraps.ldcCondyToIndy`, condy bsmArgs flattened via
  `nestedCondyInIndy`. `fixPrivateMethodsInInterfaces`: `invokeinterface`
  self-calls to private interface methods — including `LambdaMetafactory`
  impl handles — rewritten to `invokespecial`.
* **9 → 8** — none (`otherTransforms` is a no-op; the private-interface-
  method fix lives at 11 → 10 where it normalizes the shape).
* **8 → 7** — `downgradeInterfaces`: interface static/default/private
  methods are moved to a `<I>$jvmdg$StaticDefaults` inner class (abstract
  decls written back for defaults), callsites retargeted (`invokestatic`
  owner swap; `invokespecial` → `invokestatic` with the receiver as arg 0;
  indy `Handle` args likewise), and implementors missing a default get a
  synthetic bridge to the moved body.
* **7 → 6** — indy lowering: each distinct indy becomes a
  `jvmdg$indy$<name>$<n>` `CallSite` field + same-named private static
  method doing `callSite.getTarget().invokeExact(args)`; the bootstrap is
  replayed once in `<clinit>`. `ldc <Handle>` → `jvmdg$handle$<name>$<n>`
  fields resolved via `Lookup.findGetter`/`findVirtual`/…; `ldc <method
  Type>` → `jvmdg$methodtype$<n>` fields; a `jvmdg$lookup` `Lookup` field
  backs both. Resolved members are recorded via `@ReflectionReferences` on
  `<clinit>` so the shade closure keeps them.
* **6 → 5** — all stack-map `FRAME` insns removed.
