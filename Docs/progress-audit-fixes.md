# Audit Fixes — feature/core-overhaul

## RESUME

**Done + pushed**: audit verified → 3 fix commits landed (Phases A–C of the
megaplan). All findings re-verified against code before fixing; F6 was
over-corrected (throw → back to log with cause chains — per-member
resilience is the designed contract, `brokenMemberDoesNotAbortOtherRegistrations`).

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

**Next (Phase D, in order)**:
1. **F1 extras quadratic** — `ctx.extras` is the stage's accumulating OUTPUT
   worklist (`TransformPipeline` binds `ctx.extras = next` per stage and
   primaries land via `next.enqueue`), so `ExtrasApplyTransform.apply`
   re-runs the delegate over every prior class per primary: O(N²)×3 wrapped
   transforms per boundary + re-transforms already-transformed primaries
   (no idempotence contract). Fix: `TransformWorklist` drain-marker —
   snapshot only entries enqueued *during* the current primary's pass
   (`entriesSince(mark)`), keep `replace` semantics, handle extras-of-extras
   (drain to quiescent). Test: counting-transform asserting ~O(N+E) calls.
2. **F14 dual-path** — rebuild `VersionProvider.downgrade()` by composing
   the step transforms (it drifted: no `@JvmDowngraded` stamp); check
   unimined for external `downgrade()` overrides first.
3. **F13/F16/F18** — move `stubClasses`/`insertAbstractMethods`/handle-bsm
   bodies out of `VersionProvider` into `version/steps/*`; shared-runtime:
   one `resolveDeclared` core + one `paramClasses` walker (RetroTypes vs
   ReflectionTable), collapse `otherTransforms`/`getStubMapper` chains,
   shared supertype-closure helper.
4. **Backport check** — F2/F4/F5/F9/F11 also affect `main`; verify + commit
   there if user wants.

**Watch**: `TestDowngrade` (TestKit) fails on this mounted FS —
`fileHashes.bin Invalid argument`, environmental, not a code failure.
`memberSince` test needs rebuilt api jar on classpath (jar picks up the
new resource via `from(genStubIndex)`).

## Checklist

- [x] Verify all ~25 findings against code (megaplan verdicts)
- [x] Phase A guards/deletes
- [x] Phase B wrong-output fixes + PR #55 test
- [x] Phase C memberSince parity
- [ ] Phase D1 extras worklist drain-marker (F1)
- [ ] Phase D2 downgrade() dual-path unify (F14)
- [ ] Phase D3 VersionProvider decomposition + shared-runtime dedup (F13/16/18)
- [ ] Backport to main check
- [ ] Full :testing:test + CI green after all phases
