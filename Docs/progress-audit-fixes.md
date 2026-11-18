# Audit Fixes — feature/core-overhaul

## RESUME

**Gradle-plugin overhaul landed** (uncommitted atop E6's diff on
`feature/core-overhaul`, `:gradle-plugin:test` 16/16 green incl.
two-run config-cache reuse): `DefaultFlags` BuildService **deleted** —
the extension now implements `ShadeFlags` directly (managed props
generated like on tasks) and every task/spec/transform-param
`convention(extension)` — same provider chain, minus a service that
was only ever a state bag. `apiJar` is a real `ExtractApiJar` task
(`build/jvmdg-api/`) — the old side-effecting provider wrote into
`.gradle/` and self-invalidated the CC entry once per store.
`dg()`'s `downgradeTo.get()` eager read is gone: component-rule
registrations queue into `pendingModuleRules` and land in
`afterEvaluate` (or immediately if `dg()` runs post-eval), so
`jvmdg {}` blocks after `dg()` still win. `ConventionTask` →
`DefaultTask` (incl. buildSrc's `GenerateCtSymTask`), `DowngradeFiles`/
`ShadeFiles` declare a real `@OutputDirectory outputDir` (mapped
`-1/-2` disambiguation kept) instead of setter-side `outputs.dirs`,
task classpaths use `@Classpath`, `defaultTask`/`defaultShadeTask`
are nullable providers wired lazily (`named<Jar>` +
`matching{name=="shadowJar"}.configureEach`), dead
`version`/`isRefreshDependencies` vals + `ServiceReference` import
swept, `gradle-plugin:jar`'s `:java-api:jar` wireup uses
`flatMap { archiveFile }` (no eager `.get()`).
**Env note**: m2shared is ntfs3 with post-dirty-shutdown index
damage — `fileHashes.bin` inodes returned EINVAL until caches were
unlinked; `ntfsfix` ran already (mount works); real fix = Windows
`chkdsk` when convenient.
**Next**: P1 indy-dispatch benchmarks (megaplan), or deeper
catalog/engine boundary shims. All pushed through `03ccccf2`; E6 +
gradle-plugin overhaul uncommitted pending user word.

**E6 landed** (on `feature/core-overhaul` atop pushed `03ccccf2`, full
matrix green — 6m15s `:testing:test`): the 22 `Java<N>Downgrader`
classes are now **emitted by `ProviderGenerator`** into the api jar —
ctor carries the version triple, `registerBoundaryTransforms()` news up
each enumerated `jvmdg/j<N>/transform/` impl at the END of the init
window (ctor-time instantiation killed every runtime launch —
ServiceLoader provider construction re-enters the half-built
downgrader), generated `init()` carries one `stub(Marker.class)` per
marker class, j6/7/8 keep their ensureInit "VERY incomplete" warns
(F_SAME frame needed — V1_8 verifier).
The entire resource layer is deleted: `META-INF/jvmdg/providers`,
`stubs/j<N>`, `transforms/j<N>`, `dormant-stubs` all gone (annotation
presence IS the registration; dormant = don't annotate). `genStubIndex`
constructs generic stand-in providers from its `providers-csv` arg
(real classes can't exist on its own classpath — chicken/egg);
`VersionProvider.providerKey()` decouples serialized keys
(stubs.idx/excludes/member-since rows) from the gen-time impl class.
`AllVersionDowngrader.preInit` contributor loading moved to
`VersionProvider.preInit` (generated classes don't override it).
`init()` is the empty SPI fallback for external providers again.
**Next**: P1 indy-dispatch benchmarks (megaplan), or deeper
catalog/engine boundary shims. All pushed through `03ccccf2`; E6
uncommitted pending user word.

**Done + pushed**: all megaplan phases landed (A–D), backports pushed to
`main`. All findings were re-verified against code before fixing; F6 was
over-corrected (throw → back to log with cause chains — per-member
resilience is the designed contract,
`brokenMemberDoesNotAbortOtherRegistrations`).

- `9fc7eaa9` Phase A — IO guards (tmp jar leak, same-path in/out, single
  `.class` file roots now transform), `split("\t",-1)` parity,
  `defaultShadePrefix` unification, dead-code deletes
- `2c546455` Phase B — api-cache hash covers all bytecode-affecting flags,
  shade chain installs signature-strip+manifest, generated-dispatch classes
  keep mechanical TypeInsn remap, preview-version slot masking,
  PR #55 regression test (`interfaceConstantsDontEnterMemberSynthesis`,
  verified failing on unfiltered code)
- `7a71ff76` Phase C — `META-INF/jvmdg/member-since` flattened by
  `StubIndexWriter` (new arg[11] + gradle arg), `DerivedMemberSince` loads
  it on the scan path → memberSince parity across load paths
  (`TestDerivedMemberSince`)
- `f44eaf8a` Phase D/F1 — `TransformWorklist` insertion journal +
  `extrasCursor` drain-marker; `ExtrasApplyTransform` drains only entries
  enqueued since the last drain (journal tags pipeline outputs vs extras,
  `enqueueOutput` at the two pipeline sites). O(N+E) not O(N²);
  extras-of-extras still defer to the next primary's pass. Regression
  tests: `extrasPassDoesNotRetransformPriorOutputs`,
  `deferredExtrasDrainAtNextPrimary`
- `75aa31a8` Phase D/F14 — `initClass` extracted on `VersionProvider`
  (provenance stamp + version + module-info requires) shared by
  `ProviderInitTransform` and `downgrade()` — the legacy path now stamps
  `@JvmDowngraded`; SPI escape hatch (`ProviderStep`) retained
- `1d50a176` Phase D/F13+16+18 — `stubBSMArgs`/`stubHandle`/`stubCondy` +
  `HandleMethodNode` → `version/map/HandleStubber` (provider keeps thin
  delegates — `stubHandle` is protected SPI used by `Java7Downgrader`);
  `RetroTypes.resolveDeclared` core shared by `resolveStubMethod`/
  `invokeStub`/`DispatchRuntime.resolveImpl`; `primClass` shared;
  `supertypeClosure` + `declaresMethod` extracted on VersionProvider

**Backport to `main`** (`8b7da737` + `b739d657` stray-helper cleanup):
F4 same-path guards (ApiShader + Main), F9 single-`.class` filename root
(main crashes `substring(0,-6)` — worse shape than the branch's silent
passthrough), F11 tmp-jar leak. F2/F5 don't apply — main's flag set
predates the omitted hash fields and main's shade path already strips
signature files.

**Resolved as designed**: F17 `otherTransforms` arity chain — providers
override at five different levels (Java7/25@7-arg, Java8@4-arg,
Java11/15@3-arg, Java6/9/16/17@1-arg); the chain is the SPI hook
mechanism, kept. F2/F5 flagged above. F20 done — `TestShadeClosureBarrier`
covers T/M/C(F+R) rows, multi-spec pipes, `-` placeholders, malformed
column counts/tags, unparseable specs, and the `split("\t",-1)` trailing-
empty-column edge.

**Full `:testing:test` matrix**: BUILD SUCCESSFUL (3m22s) — all
downgrade/runtime/shade/parity suites green post-Phase-D. The earlier
`TestDowngrade` TestKit flake did not recur.

**Watch**: `TestDowngrade` (TestKit) fails on this mounted FS —
`fileHashes.bin Invalid argument`, environmental, not a code failure.
Stray `gradlew-lf` add+remove pair sits in main history (`8b7da737` +
`b739d657`) — squash via force-push only if user asks.

## Checklist

- [x] Verify all ~25 findings against code (megaplan verdicts)
- [x] Phase A guards/deletes
- [x] Phase B wrong-output fixes + PR #55 test
- [x] Phase C memberSince parity
- [x] Phase D1 extras worklist drain-marker (F1)
- [x] Phase D2 downgrade() dual-path unify (F14)
- [x] Phase D3 VersionProvider decomposition + shared-runtime dedup (F13/16/18)
- [x] Backport to main check + apply (F4/F9/F11)
- [x] F20 ShadeClosureBarrier row-parser coverage (`TestShadeClosureBarrier`)
- [x] Full :testing:test matrix after all phases — BUILD SUCCESSFUL;
  CI remains the remaining green signal
- [x] E1 StubCatalog extraction (`f489faf2`) — registry data + resolver
  family into version.StubCatalog; public field aliases kept
- [x] E2 BoundaryTransform migration (`09ceafa8`) — all provider
  otherTransforms bodies -> named j<N>.transform classes, discovered via
  META-INF/jvmdg/transforms/j<N> resources; shared indy shapes in
  version.indy.IndyShapes; Java9 identity override deleted
- [x] E3 provider-config emission — init() registrations journaled to
  catalog.initRegistrations; StubIndexWriter emits
  META-INF/jvmdg/{providers,stubs/j<N>}; ensureInit replays stubs/j<N>
  instead of init() when the resource exists (init() fallback retained;
  j9's init() had a latent duplicate registration, dedup'd in emission)
- [x] E4 gen-time classpath enumeration + parity gate —
  `version/idx/StubClassEnumerator` ASM-scans the gen classpath for
  `jvmdg/j<N>/` classes carrying StubScanner marker annotations (or
  `BoundaryTransform`), recording InnerClasses outer-chains; parity model
  in `StubIndexWriter`: every marker class must be covered by a
  registered anchor (itself or an enclosing registered class — matches
  `scan()`'s `getDeclaredClasses()` recursion) and every anchor must
  cover ≥1 marker (else dead registration → build error). Uncovered
  markers are latent missing registrations — emitted to
  `META-INF/jvmdg/dormant-stubs` + build-log warning, not fatal
  (activation is a behavior change needing stub-test coverage).
  Fixes the gate surfaced: deleted 5 dead `stub()` calls registering
  zero-annotation classes (j20 `J_L_R_Field`; j8 `J_L_ThreadLocal` +
  `J_U_S_{Double,Int,Long}Stream` vocabulary interfaces — classes kept
  as signature types), annotated `J_L_ThreadLocal.withInitial` (real
  impl whose @Stub was never written — now dormant-tracked).
  12 dormant classes tracked: j8 `J_U_Comparator`,
  `J_U_PrimitiveIterator` family ×7, `J_L_ThreadLocal`; j9 `J_L_Long`;
  j15 `J_N_C_S_SelectorProvider`; j16 `J_U_L_LogRecord`.
  Transform parity added: resource-loaded `boundaryTransforms` must
  equal classpath-discovered impls (all 8 transforms/j<N> files pass).
  **E3 regression found by bisect**: `TestCtorRewrite`'s
  `TestProvider(V11→V1_8)` now replays `stubs/j11` (real `FileReader`
  ctor entries collided with fixture assertions) — fixed by
  `debugSkipStubs.add(V11)` in its `provider()` helper; other
  TestProvider files tolerate the registry or intentionally load it.
  Full `:testing:test` matrix green (3m12s, 1420 tests).
  **Follow-up**: dormant-stub activation triage (each needs coverage
  before registering; E5 datagen must decide anchors-only vs
  anchors+dormant replay); transforms/j<N> generation replacing the
  handwritten files still open.
- [x] E5 enumeration-driven registration — all 22 provider `init()`
  bodies deleted (providers are ctor-only classes now); `init()` is a
  concrete no-op SPI fallback on VersionProvider. `stubs/j<N>` emits
  `discovered markers − dormant-stubs` name-sorted (nested marker
  classes listed directly — identical registry to anchor recursion;
  marker-less anchors dropped). `dormant-stubs` flipped to an authored
  input resource (java-api/src/main/resources) — exclusion is explicit,
  a stale row fails the build, and adding a stub class to `j<N>`
  auto-registers it (forgotten-registration bug class closed).
  `genStubClasses` is the gen-time seam: the writer sets each provider's
  enumerated list before ensureInit so registration lands INSIDE the
  init window — a post-hoc `vp.stub()` top-up missed
  AdapterForwarders/DerivedExclusions/DerivedMemberSince/DerivedGained
  and produced a corrupt idx (StubParityTests caught it:
  IncompatibleClassChangeError on j11 `Collections.shuffle` + stale
  memberSince profiles). `replayRegisteredStubs`-false + bundled
  provider + `providers` resource present → missing-list warn.
  Registration-order note: emitted lists are name-sorted vs init()'s
  call order — registry result verified identical by the matrix.

- [x] Dormant-stub activation — all 12 `dormant-stubs` rows removed
  (file is header-only again): j8 `J_U_Comparator`, `J_U_PrimitiveIterator`
  family ×7, `J_L_ThreadLocal`; j9 `J_L_Long`; j15
  `J_N_C_S_SelectorProvider`; j16 `J_U_L_LogRecord` — every member got
  synthesized parity probes, none needed manual claims or `DEFERRED`
  entries (j8 impls activate only at the deferred 8→7 boundary, but the
  probes compile/run on the host JDK so coverage registers). Activation
  surfaced a real misregistration: `J_L_ThreadLocal.withInitial`'s bare
  `@Stub` consumed the first param as `this` (per StubHandler's
  first-param-is-receiver convention) and registered
  `Supplier.withInitial()LThreadLocal;` — fixed with
  `ref = @Ref("java/lang/ThreadLocal")` (static member stubs need an
  explicit owner ref). Known pre-existing gap flagged:
  j12 `J_L_C_ConstantDesc.describeConstable` resolved via manual claim
  (`constantDescDefaultDescribeConstable` in MiscTests): the adapter's
  default has no real counterpart (ConstantDesc never declared it in
  any JDK — Constable/ConstantDesc are disjoint hierarchies), but
  J_L_C_ClassDesc/J_L_C_MethodTypeDesc receivers inherit it and
  MethodTypeConstable.describeConstable dispatches through it, so a
  MethodType.methodType().describeConstable() probe covers the member
  honestly. untestable.txt is now header-only. Caveat found en route:
  J_L_C_ConstantDesc extends J_L_C_Constable models a hierarchy the
  JDK never had — downgraded `instanceof Constable` on ConstantDesc
  impls answers true where native says false; flagged, not fixed.
- [x] E6 generated provider classes — `version/gen/ProviderGenerator`
  emits `Java<N>Downgrader.class` (V1_8, +.java dump) per version-table
  row: `super(in,out,pri)` ctor + `registerBoundaryTransforms()`
  override carrying `boundaryTransforms.add(new Impl())` per enumerated
  `j<N>/transform/` impl (name-sorted, public-no-arg-ctor contract) +
  `init()` = `stub(Marker.class)` per enumerated marker (name-sorted;
  nested names `$`-joined). Transform instantiation lives in the new
  `VersionProvider.registerBoundaryTransforms()` hook at the END of the
  init window (pre-sort) — NOT `<init>`: the first cut put the adds in
  the ctor and every runtime launch died `ServiceConfigurationError` —
  provider construction inside `collectProviders`' ServiceLoader pass
  loaded `j<N>/transform/*` through the DowngradingClassLoader, whose
  transform needs `versionProviders` (still null → NPE). Deferral
  preserves the old `loadBoundaryTransforms` timing: classes transform
  against the complete registry. j6/7/8 get the ensureInit
  incomplete-stubs warn override (required `visitFrame(F_SAME)` at the
  branch target — V1_8 strict verifier, caught by selfDowngrade).
  `StubIndexWriter` now constructs `GenProvider` stand-ins from the
  `providers-csv` arg instead of ServiceLoader (the emitted classes
  can't be on its own classpath), injects them into the
  cross-boundary `ClassDowngrader` via new `addProvider`, and writes
  the `META-INF/services/...VersionProvider` file itself.
  `VersionProvider.providerKey()` (default `getClass().getName()`,
  GenProvider overrides to the emitted FQCN) keys stubs.idx, excludes,
  member-since, reflection.idx rows + `loadStubIndex` lookup —
  caught by `TestStubIndex` when three writer sites still used
  `getClass().getName()` (excludeChild [] vs expected). Deleted:
  all 22 authored providers, `providers`/`stubs/j<N>`/`transforms/j<N>`/
  `dormant-stubs` resources, `replayRegisteredStubs`/
  `loadBoundaryTransforms`/`warnMissingGeneratedList`, the
  `TestCtorRewrite` `debugSkipStubs` workaround (TestProviders no
  longer inherit the bundled registry — isolation restored).
  `genStubClasses` stays as the writer-side seam: GenProvider has no
  compiled init() to run, so the writer sets the enumerated list and
  ensureInit registers it inside the init window (E5's
  AdapterForwarders/derived-loaders ordering constraint unchanged);
  at runtime the generated init() replays the same list.
  `AllVersionDowngrader.preInit`'s ReflectionContributor ServiceLoader
  moved to `VersionProvider.preInit` since generated providers don't
  override it. `META-INF/jvmdg/` now holds exactly 7 runtime tables +
  stubs.idx + member-since. Full matrix green.
