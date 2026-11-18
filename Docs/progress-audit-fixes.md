# Audit Fixes — feature/core-overhaul

## RESUME

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
- [ ] E4 gen-time classpath enumeration of j<N> annotation classes +
  parity assertion vs recorded registrations (then transforms/j<N>
  generation replaces the handwritten files)
- [ ] E5 delete init() bodies; optional generated provider classes
