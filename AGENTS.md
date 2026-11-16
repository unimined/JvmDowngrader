# JvmDowngrader — Agent Notes

Gradle build (Kotlin DSL). Main/shared source compiles on the
**`stubToVersion` toolchain** (currently 27) and is **self-downgraded to
v52** by this build's own ZipDowngrader before packaging — core/agent/CLI
then require a Java 8+ **runtime** (`mainVersion=8`), while downgrade
*targets* still go down to 7/6/5 (the 8→7 stub work is separate and
deferred). Java 8 language/APIs remain the norm in `src/main`/`src/shared`;
anything newer needs stub coverage at the 8 boundary. Test code targets
whatever toolchain the test JVM uses (Azul `stubToVersion`).

## Building & testing

- `./gradlew :testing:test` — unit tests + end-to-end downgrade matrix across
  every JDK from `testTargetVersion` to `stubToVersion` (Azul toolchains
  auto-provisioned via foojay-resolver).
- **Self-host graph** (CG-5): `compileJava`/`compileSharedJava` emit v71 →
  `rawCoreJar`/`rawSharedJar` → `selfDowngrade*` JavaExecs (own
  ZipDowngrader on a j27 launcher, `selfHostApiJar` as the api input —
  the dedicated host jar breaks the `apiJar → selfDowngrade → apiJar`
  cycle) → `core-downgraded-8.jar`/`shared-downgraded-8.jar` under
  `build/selfhost/`. `jar`/`shadowJar` pack the downgraded zip plus the
  v52-downgraded api (`downgradeJar8`) so standalone `java -jar` resolves
  gained dispatchers/stubs; `dropRawToolchainClasses` drops any class still
  >52 from other spec sources. `compileSharedV52`/`compileMainV52` are
  classpath-only j8 compiles of the same sources for pre-27 consumer
  toolchains (java-api, gradle-plugin, javac-plugin) — never shipped.
  `selfHostSmokeTest` runs the fat jar's `Main --version` on a j8
  launcher.
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
  so native vs downgraded/shaded output diffs line-for-line (set
  `-Djvmdg.corpus.debugExc` on the run JVM to also dump `EXC:` stack
  traces — the line protocol stays clean). Classes become
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
- `--noIndy` (`Flags.noIndy`) appends java-api's `NoIndyContributor` stage
  `downgrade:noIndy` at `AFTER_VERSION_CHAIN`: union of all providers'
  `indyDesugars` newest→oldest until each indy is gone (normalizers like
  j23 `typeSwitch` run ahead of older real desugars), unregistered bsms →
  generic `jvmdg$indy$` callsite-field lowering (same shape as the j7→6
  boundary) + deduped `DiagnosticSink` warn, `ldc <ConstantDynamic>` →
  `jvmdg$condy$N` fields resolved once in `<clinit>`. Output contains zero
  `InvokeDynamicInsnNode`/`ConstantDynamic`. Unit-tested in `TestNoIndy`.
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
             └─ version/     DowngradeRegistrar (implements the
                  PipelineRegistrar slot a future UpgradeRegistrar shares)
                  emits the stage list per-VersionProvider boundary
                  (decomposed version/steps/:
                  ProviderInit → StubClasses → StubMethods → InsertAbstracts
                  → OtherTransforms → FlushWarnings; ProviderStep retained
                  only for providers overriding downgrade()). stages(Set)
                  builds the union chain for mixed-version input sets.
                  All version compares go through Utils.majorClassVersion —
                  ClassNode.version is minor<<16|major and javac
                  --enable-preview sets minor 0xFFFF; preview inputs are
                  rejected unless --allowPreview (ClassDowngrader byte path
                  + PipelineJarStep's finals gate), then route through their
                  major's chain, and StubCondition.PREVIEW entries activate.
                  TransformContributor SPI (ServiceLoader, class-name-sorted)
                  contributes Stages at BEFORE_/AFTER_VERSION_CHAIN in
                  stages() and POST_EMIT at PipelineJarStep's stage tail;
                  ReflectionContributor SPI owns the reflection-stub surface
                  (api jar's CoreReflectionContributor registers J_L_Class/
                  J_L_I_MethodHandles$Lookup/J_L_R_Method/J_L_R_Constructor
                  from META-INF/services) — AllVersionDowngrader.preInit
                  applies every discovered contributor per boundary.
             └─ version/map/ StubRegistry (per-owner store) + StubScanner
                  (reflective @Stub/@Modify/@IndyDesugar/@Adapter
                  registration) +
                  MemberStubTransform (emission) + StructuralRemapTransform
                  (mechanical type-slot remap + retro-supertype rewrite:
                  class-stubbed instanceof/checkcast without a jvmdg$ hook
                  emits a single invokestatic to the stub's generated
                  <stubType>$jvmdg$retro dispatcher — .wrap for
                  OWN_INTERFACE adaptation, .cast/.isInstance/array forms
                  elsewhere — gated on Adapter.mode
                  alone — scan/idx load paths must emit identically, so
                  never gate on idx-derived fields; gained-supertype
                  interfaces route the same sites to their gen/gained/<I>
                  structural helpers via TypeRemap.gainedFor with the
                  authored class version as ceiling; remapClassCalls runs
                  over MethodAnalysis — the per-method lazily-computed
                  fused Analyzer artifact (FusedValue = producers+type in
                  one pass; share the holder with any consumer needing
                  stack info — class queries route ldc'd-retro
                  Class.isInstance/cast/isAssignableFrom receivers, and
                  aastore sites whose array element type is a gained
                  interface route the value through jvmdg$aastore so raw
                  members wrap instead of ArrayStoreException-ing) +
                  MemberResolver/TypeRemap
                  (ClassMapping implements both — resolver graph only);
                  INVOKEVIRTUAL/INVOKEINTERFACE sites resolved to an
                  instance stub emit an invokedynamic bootstrapped by
                  stub/DispatchRuntime (DispatchSites owns the contract):
                  bsmArgs carry the member MethodType + impl owner Class +
                  "name desc" String (lazy — no Handle constant); a
                  per-callsite ClassValue PIC resolves the receiver's own
                  member via findVirtual — user overrides win, no marker
                  interface is injected; the site also declares the impl
                  via @ReflectionReferences (desc remapped through the
                  provider chain → stand-in vocabulary) so the shade
                  closure keeps it; INVOKESPECIAL sites keep the impl;
                  member-stub misses on @Adapter(signaturePolymorphic=…)
                  owners (VarHandle's access modes) emit a second indy
                  contract — DispatchSites.polymorphicInsn: indy site-name
                  (stubType, site-args…)site-ret, bsm = the adapter's
                  jvmdg$bootstrap hook + no bsmArgs — the hook derives the
                  impl signature from the callsite name+desc at link time
                  (name-mangle/ref-erasure/ret rules) and asType does the
                  casts/boxes a @Modify splice used to emit inline
                  ClassStubEntry bakes hook members + the stub-is-interface
                  `itf` bit at registration (idx-serialized): emitters never
                  reflect on stub classes — class loading in the transform
                  path sees pre-downgrade api classes (UCVE) or misses
                  synthetic test fixtures entirely
             └─ version/idx/ StubIndex — META-INF/jvmdg/stubs.idx codec
                  (deterministic, format 7 + jvmdg-versioned — class-stub
                  records carry optFQMND polymorphic + the
                  signaturePolymorphic name set since 7); StubIndexWriter
                  serializes the reflective scan at api-jar build time
                  (genStubIndex JavaExec; stamps memberSince per registered
                  member + ClassStubEntry.implementedBy per adapter +
                  derived excludeChild per member stub from the genCtSym
                  JDK-surface index — ct.sym entries are read on the
                  live enumeration, duplicate-name .sig rewrites break
                  deferred opens — and flattened into META-INF/jvmdg/
                  retrotypes + META-INF/jvmdg/excludes + META-INF/jvmdg/wraps
                  + META-INF/jvmdg/gained + META-INF/jvmdg/gained-calls
                  + META-INF/jvmdg/reflection.idx,
                  the RetroTypes runtime tables + the ReflectionTable
                  type-remap rows + the scan path's
                  derived-exclusion copy + @Adapter(impls=…) member→impl
                  pairs flattened upward through the adapter interface
                  graph from vp.adapterImpls — build-time side data the
                  idx never carries);
                  providers idx-load at ensureInit, falling back to the
                  scan (jvmdg.debug.noStubIdx forces it);
                  DerivedExclusions unions the excludes resource into
                  scan-built entries so both load paths resolve identically;
                  DerivedGained load-filters the gained rows per provider
                  boundary (interface real at the final target — imin ≤
                  flags.classVersion — member binding inside the boundary)
                  into vp.gained — the same resource feeds both paths;
                  vp.gained also drives user-class interface injection in
                  stubClasses (a supertype closure containing a gained
                  member T gets the real I added — the resolver sees the
                  source-version hierarchy where T already is I, so member
                  intersection is the only signal) and insertAbstractMethods'
                  gained bridge pass (each gained-calls member the class
                  doesn't serve natively — gated by ClassMapping.getStubFor —
                  gets a public bridge invokestatic-ing the impl handler
                  directly; never the dispatcher, since the bridge's own
                  declaration would mark the class and recurse)
             └─ version/gen/ ApiGenerator + GenContext — api-jar build-time
                  codegen run inside genStubIndex (post-population, same
                  registry data the runtime derives): MarkerGenerator
                  ($Marker interfaces) + RetroDispatchGenerator
                  (<stubType>$jvmdg$retro — real-bytecode dispatch/construction
                  so C2 inlines and scalar-replaces where reflection
                  couldn't; wrap = adapter instanceof → ordered impl
                  instanceof/new chain under catch(LinkageError) →
                  RetroTypes.wrapImpl tail; isInstance/cast/array forms =
                  adapter/marker/impl-member instanceofs → table tails;
                  isAssignableFrom) + GainedDispatchGenerator
                  (xyz/wagyourtail/jvmdg/gen/gained/<mangled-I> — one
                  helper class per gained-supertype interface + its $Wrap
                  sibling; a gained supertype is a JDK class that acquired
                  an interface after the target: instanceof/checkcast/
                  class-query sites can't verify so they route to the
                  jvmdg$isInstance/cast/isAssignableFrom/isArrayInstance/
                  isArrayAssignableFrom structural methods, each carrying
                  a trailing int ceiling = the calling class's authored
                  version; member-CALL sites instead emit an
                  invokedynamic bootstrapped by stub/GainedRuntime
                  (bsmArgs = srcName, "name desc", ceiling — desc erases
                  ref types to Object + trailing checkcast) since the
                  generated dispatcher's catch(LinkageError)-per-call cost
                  measured ~200-600x the PIC path on missing-I targets;
                  dispatch machinery (stub/ + gen/ + $jvmdg$/$Ov$ owners,
                  GainedSites.isDispatchMachinery) emits neither —
                  indy inside bootstrap self-recurses and its member calls
                  all resolve natively anyway; --noIndy lowers the site
                  via stub/all/GainedRuntimeDesugar → invokestatic
                  RetroTypes.gainedCall; $Wrap supplies the real I-typed
                  checkcast result since member objects can't implement I,
                  its members pack self/args and tail-call
                  RetroTypes.gainedCall directly, declaring
                  @ReflectionReferences for the gainedCall-resolved stub
                  sites — reflective reach has no bytecode edge for the
                  shade closure; ref descs carry the
                  provider-chain remap since the api index keys members in
                  post-remap stand-in vocabulary) + MemberForwarderGenerator
                  (<adapter>$jvmdg$fwd — one class per OWN_INTERFACE adapter;
                  AdapterForwarders (version/) is the shared contract both
                  producers enumerate so scan and gen can't drift:
                  AdapterForwarders.registerAll — run post-scan inside
                  ensureInit between init() and DerivedExclusions, since a
                  descendant adapter's impls pull up to ancestors and
                  adaptability is only decidable once every adapter has
                  registered — writes a member stub per forwardable
                  interface instance method (statics, private
                  and synthetic members excluded; hand-declared @Stubs win)
                  pointing at <name>(Object self, declared-args…)
                  declared-ret, gated by adaptable() (own wrap= or impls
                  anywhere in the adapter subtree — impl-less interfaces
                  like the J_N_H_* adapters and the JumpableGenerator
                  branch get nothing; member callsites there just remap to
                  a raw invokeinterface on the stand-in), and the
                  generator emits the bodies —
                  wrap(self) via the adapter's $jvmdg$retro, stand-in-typed
                  args re-coerce through their own adapter's wrap (the arg
                  may carry a real-JDK member object), then invokeinterface.
                  Member keys reuse StubHandler's coercion so they match
                  what a hand-written @Coerce forwarder registered — the
                  swept @Stub forwarders on Constable/ConstantDesc/
                  TypeDescriptor/RandomGenerator were exactly this shape;
                  factories, jvmdg$wrap helpers and @DefaultImpl hooks stay
                  authored). Generated classes are ordinary api
                  classes — reference indexing, prefix shading, pruning and
                  runtime downgrade treat them like any other api ref; V1_8,
                  hand-authored frames, deterministic, hand-declared-wins;
                  .java inspection dumps under build/generated/gen-src
             └─ stub/RetroTypes (src/shared, ASM-free) — retro-supertype
                  runtime: name-keyed ClassValue membership over the
                  merged retrotypes resources — the fallback tier for the
                  bulk JDK-member name sets (hundreds-to-thousands can't be
                  an instanceof chain); wrap() fast-paths through the
                  context's $jvmdg$retro dispatcher (per-loader MethodHandle
                  cache, gated on the named row's stub matching the context
                  — a mismatched context keys on the name instead) and
                  wrapImpl() is the table path the dispatchers tail-call:
                  ordered member→impl rows (ctor-derived, ctor-cached per
                  caller loader, adapter-typed objects pass through, the
                  row whose stub column names the context wins so two
                  prefixed jars can't steal each other's impls) — impl
                  resolution goes through the context's loader so a
                  runtime-downgrading classloader transforms them like any
                  other api ref; adapters opt non-stub impls
                  in via a nested $Marker interface — injected by
                  stubClasses into implementor/extends clauses of SUPERTYPE
                  sources (the type remaps away, the marker restores
                  membership); SwitchBootstraps.typeSwitch labels stay
                  unremapped in bsmArgs and the stub emits
                  isInstanceOrFallback per label; member desc collisions
                  after the type remap are a hard error in stubClasses;
                  gained tables: GAINED (I → member:since) answers
                  gainedMember/gainedAssignable at a ceiling, GAINED_CALLS
                  (I.member → T → per-ceiling declarer + stub site) drives
                  gainedCall — native declarer first, registered stub
                  second, AbstractMethodError on unresolvable rows;
                  receiver-name resolution bridges stand-ins through
                  MEMBER_OF (member → retro-owner reverse of MEMBERSHIP,
                  populated in register alongside NAMES) so an impl's
                  closure — which names the stand-in, never the source
                  member — still matches gained rows (HttpClient impl →
                  J_N_H_HttpClient → java/net/http/HttpClient:65);
                  reflective sites resolve through the caller-dispatcher
                  class's loader (context param) like wrap/wrapImpl, so a
                  runtime-downgrading loader transforms them
             └─ stub/ReflectionTable (src/shared, ASM-free) — the
                  Class.forName + getMethod runtime: T rows (source →
                  stand-in target, since) + M rows (owner, callsite name +
                  desc, i|s, impl owner/name/desc, since, JDK mods) from the
                  merged META-INF/jvmdg/reflection.idx resources, applied
                  only inside their (runningJvm, authored] window — inert
                  rows pass through to native lookup (real class/member or
                  honest CNFE/NSME). Descriptor-form array names remap the
                  object element; the one-arg forName site's resolution goes
                  through the callsite class's loader — Stub.callerClass
                  strips a trailing Class param at registration (like
                  downgradeVersion's int) and MemberStubTransform pushes
                  ldc <containing class> before the version ldc. getMethod
                  walks supertypes + BY_TARGET (a forName stand-in like
                  J_L_Module reaches rows registered under java/lang/Module);
                  getDeclaredMethod stays on the class; caller param Classes
                  match row descs by leaf name or T-row reverse; impl descs
                  stay source-shaped in the table and remap through T rows
                  at resolve time (the shipped api jar holds the stand-in
                  signature). The returned object is the impl's own Method —
                  StubMethodView's identity map presents the source view:
                  receiver shifted out of arg0 for i rows on invoke,
                  source-desc getParameterTypes/Count, getName,
                  getDeclaringClass (the owner's runtime stand-in),
                  getModifiers; impl handlers carrying transform-injected
                  args past the member signature get them fabricated at
                  registration (int → authored version, Class → the looked-up
                  class — the same values the callsite ldcs carry; any other
                  extra shape leaves the row unresolved); unregistered
                  Methods pass straight through. getMethods/
                  getDeclaredMethods stay real-only. Lookup.find* shares the
                  member table — every member lookup is native-first (rows
                  by construction cover only members the running JVM lacks
                  on that owner; a real member declared on a subtype — e.g.
                  String.isEmpty vs the j15 CharSequence.isEmpty row —
                  answers native; an access failure propagates the honest
                  IAE instead of slipping the impl past it): findClass takes
                  the forName remap scoped to
                  lookupClass's loader (no init); findVirtual/findSpecial/
                  findStatic kind-filter the rows (i vs s) and answer a
                  receiver-first findStatic on the impl — findVirtual via a
                  DispatchRuntime PIC handle (extras bound to the lookup
                  context) so user overrides win, findSpecial the impl
                  directly; the impl MethodType is built
                  in runtime types from the source-shaped impl desc.
                  unreflect needs no stub (the handed-out Method IS the impl
                  — native unreflect already yields receiver-first);
                  unreflectSpecial routes view-registered Methods around the
                  static restriction to unreflect. Produced handles register
                  a StubHandleView MethodHandleInfo facade (weak identity map
                  — source owner/name/signature/REF_* kind/mods; interface
                  owners report REF_invokeInterface; reflectAs answers the
                  impl's own Method, identity-consistent with getMethod), so
                  revealDirect exposes the source view instead of impl
                  internals — unregistered handles resolve natively.
                  stubHandle's constant
                  methodref path mirrors the same route: virtual-family
                  Handle tags on dispatchable stubs get a wrapper emitting
                  the same indy callsite, and
                  transform-injected args (callerClass ldc, version ldc) are
                  emitted inside the desc-mismatch wrapper the callsite would
                  have carried — Class::forName methodrefs resolve through
                  the impl instead of linking the raw JDK member. C rows
                  (owner, orig desc, R|F, toDesc, keep map, converter|pre|
                  post specs with orig-arg indices + :v version tail, since,
                  mods) carry the @InitRewrite/@InitFactory spec into
                  findConstructor/getConstructor/getDeclaredConstructor/
                  unreflectConstructor — owner-exact (no supertype walk:
                  ctors don't inherit), native-first like the member paths,
                  and rewrite rows synthesize the callsite sequence as a
                  MethodHandle combinator chain (target findConstructor →
                  converter filterArguments right-to-left → permuteArguments
                  onto the orig args → foldArguments for void pre/post
                  hooks); getConstructor hands out the target ctor's real
                  Constructor instance identity-keyed to a StubCtorView —
                  Constructor copies are per-fetch, so only the handed
                  instance answers source params/count/mods and
                  newInstance's varargs arity-selects the synthesized handle
                  (ctor-thrown exceptions map to InvocationTargetException);
                  reflectAs returns that same instance. Supertype
                  stand-ins are
                  real JDK types; api-target and M/C impl rows prune out
                  under shading when the class isn't in the closure — miss =
                  native CNFE/NSME (dynamic reach has no bytecode edge;
                  closure roots are the CG-3e usedOwners tier).
                  getInterfaces/getGenericInterfaces filter the transform's
                  injected artifacts back out of the runtime list: gained
                  interfaces (stubClasses records each injected one in a
                  runtime-visible @GainedInterfaces String[] of binary
                  names — sibling of @RemovedInterfaces) plus generated
                  $Marker/legacy $Ov$ interfaces by name pattern (prefix-stable
                  under shading); remapped stand-ins stay — the source
                  type doesn't resolve at the target. The annotation is
                  read by NAME (getDeclaredAnnotations + reflective
                  value()), never getAnnotation(Class): the inspected
                  class and the table can sit in different loaders in the
                  agent path, so the annotation's Class identity differs
                  and the typed lookup silently returns null.
                  Interfaces the JDK serves natively but the callsite can
                  only name through a stand-in are swapped for it — the
                  same window the callsite's ldcs were remapped under,
                  (to, from] read off the caller's @JvmDowngraded stamp:
                  ProviderInitTransform writes the runtime-visible
                  {from=authored major, to=flags.classVersion} annotation
                  at the first boundary (before node.version is rewritten);
                  callers without the stamp answer the runner's native view
             └─ stub/OptionalDependency + RuntimeDowngrader (src/shared,
                  ASM-free) — probe point for stub impls that optionally
                  need the heavy core: ServiceLoader discovers
                  CoreRuntimeDowngrader (src/main, registered via
                  META-INF/services in the core jar) only when core shares
                  the api classpath; absent backend → caller falls back to
                  same-JVM behavior. Used by Lookup.defineClass (j9) /
                  defineHiddenClass (j15) — the only REQUIRES_RUNTIME
                  members, which gate callsite rewrite on enableRuntime
                  (agent/runtime path only; jar-mode leaves the raw invoke
                  so it fails honestly as NoSuchMethodError).
                  Non-ALWAYS-conditioned rows are excluded from reflection.idx
                  (ClassMapping gates on entry.condition.active(enableRuntime,
                  flags.allowPreview); StubEntry.requiresRuntime remains as the
                  boolean view of condition == REQUIRES_RUNTIME, and the
                  @Stub/@InitFactory/@InitRewrite condition attr replaces the
                  boolean — declaring both is a registration error)
             └─ stub kinds + policy — memberSince/memberKind side-maps on
                  StubRegistry (idx-populated; scan path leaves memberSince
                  empty → MISSING_API); kindOf = kind= override → modify ⇒
                  LANGUAGE → since≤target ⇒ BEHAVIOR → else MISSING_API.
                  MemberStubTransform.vetoed applies Stub-Policy
                  (full/noBehavior/mechanical) + On-Missing-Api (error/warn)
                  and scoped exclusions post-resolution — never on
                  unstubbed calls
             └─ descriptor-variant members — a member whose erased desc
                  *changes* at a later JDK registers as an ordinary member
                  stub at the version the new desc appeared: JDK 21's
                  ConstantDesc genericization added covariant
                  resolveConstantDesc redeclarations ((Lookup)T on
                  ClassDesc/MethodTypeDesc/MethodHandleDesc, memberMin=65)
                  covered by j21 J_L_C_ConstantDesc for the 56–64 window
                  (the j12 adapters declare the same covariant shapes so the
                  remapped callsite resolves below 56). A member the callsite
                  class already has must not be shadowed by a supertype stub
                  — resolution checks the callsite FQMD's own presence before
                  the parent walk (see Open items for a MethodType.returnType
                  miss under that check)
             └─ compile/shade/ ReferenceIndex (member-level edges, floor
                  slot lookup, usages inversion, BFS closure) + IndexScanner
                  (entry stream → index) + RefExtractor SPI (MemberRefExtractor,
                  RequiresResourceScanner, ReflectionReferencesScanner) +
                  ShadeRegistrar stages: scan → closure-barrier (required set,
                  inline candidates, per-slot remappers, pruned+prefixed api
                  ClassNodes, api resources, variant shading) → inline →
                  prefix → reflection-fold → module-info-strip. The scan
                  filter side-channels every extracted ref into
                  ShadeContext.inputRefs + dynamicSites (the index retains
                  api refs only) — Shade-Reflection none|usedOwners|all roots
                  api-class impls of kept reflection.idx rows from the
                  recorded owner set + Shade-Keep-Reflection patterns before
                  the closure runs (JDK-target rows never root — e.g. gained
                  supertypes map to real interfaces), so kept rows survive
                  pruning and miss=native holds under usedOwners/none
                  (dynamic-site members get a shade warning);
                  ShadeReflectionFoldTransform rewrites literal
                  forName/findClass args to the prefixed T-row target after
                  prefix — nearest-name-source only
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
             downgrade, untouched by the jar-level rework); api packages
             (providers./stub.all./gen./j<N>.) load child-first so the api
             jar can't serve raw classes when it also sits on the app
             classpath — gen. included because the gained dispatchers'
             loader is the context RetroTypes resolves api classes through
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
| Indy desugar | `@IndyDesugar(ref = @Ref("Lbsm;member;desc"))` on a `public static void` method taking `(ModifyContext)` — fires at indy sites where the member is the bootstrap (boundary + `noIndy` sweep); never intercepts direct invokes of the bsm member — those resolve `@Stub` impls instead (concat/`ObjectMethods`/`SwitchBootstraps` build real `ConstantCallSite`s over bound static helpers; `metafactory` proxies via `MethodHandleProxies`; `altMetafactory` stays `MissingStubError`). Registry lives in `StubRegistry.Owner.indyDesugars`, serialized in `stubs.idx` format ≥5. |
| Class-type redirect | `@Adapter` — feeds `TypeRemap`/`StructuralRemapTransform`; `keepInterface` drops the type from `implements`. Retro `instanceof`/`checkcast` is automatic (mode derives from `target`): every site emits a single `invokestatic` to the stub's generated `<stubType>$jvmdg$retro` dispatcher — `wrap` for `OWN_INTERFACE` casts over `@Adapter(impls=…)` — impls need exactly one single-arg ctor (the member type), and declarations pull upward to ancestor adapters; the dispatcher's impl references resolve in the caller's loader so downgrading loaders transform impls like any other api ref, and `wrapImpl`'s row selection keys the `wraps` row to the context's name so prefixed jars stay in-namespace. `X.class.cast` on an ldc'd retro receiver mirrors the same hook → `wrap=` → generated-wrap dispatch. A `jvmdg$instanceof`/`jvmdg$checkcast` static or `wrap=` factory is an explicit override for adaptation the table can't express; a `jvmdg$wrap` helper on an adapter delegates to `RetroTypes.wrap` for hand-written impl code that must coerce a member object (generated `$jvmdg$retro` classes don't exist when java-api compiles — most adapters no longer need the helper; keep it only where impl code calls it). `invokespecial` (`X.super.m`) on an OWN_INTERFACE member is never redirected — suppression derives from the owner's adapter mode in `ClassMapping.getStubFor`; `Stub.noSpecial` remains the explicit escape hatch elsewhere. Non-stub impls opt into membership via the adapter's `$Marker` interface. |
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
