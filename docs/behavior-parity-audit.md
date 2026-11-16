# Behavior-Parity Audit — "launch any Java program on an older JVM"

Audit of jvmdg runtime/transform behavior vs real-JVM semantics, produced by
megaplan phase M-4. Findings are ranked into the fix list at the bottom.
Evidence was produced against real Azul Zulu 8 (`1.8.0_462`) JVM runs with the
3.0.0 api + core jars, and by direct code audit of the stub/transform surfaces.

## M-4A — verifier legality of member→gained-interface value flow

A "gained" interface `I` is one a JDK class `T` acquired *after* the target
(e.g. `StringBuffer → Comparable` at j11, `HttpClient → AutoCloseable` at j21).
A raw `T` object flowing into an `I`-typed slot was suspected to fail
verification on old targets. **Crafted-bytecode probes (V56→V52) on real j8
disprove the broad claim and isolate the one true failure:**

| Flow shape | j8 verdict | Notes |
|---|---|---|
| invoke arg `f(Comparable)` ← `StringBuffer` | **legal** | HotSpot's verifier is lenient on *interface* assignability — it does not load the pushed class's interface set for class→interface checks, so no `VerifyError` |
| `areturn Comparable` ← `StringBuffer` | **legal** | same leniency |
| `putstatic`/`putfield` `Comparable` ← `StringBuffer` | **legal** | same leniency |
| locals at merge points + downstream `invokeinterface` | **legal + correct** | verified end-to-end: `StringBuffer` merged with a real `Comparable` then `c.compareTo(c)` → gained indy → PIC resolves the declarer row → `0` |
| `aastore` into `Comparable[]` ← `StringBuffer` | **`ArrayStoreException`** | the *runtime* element-type check does enforce — real gap |
| `checkcast`/`instanceof` on `Comparable` | **already routed** | transform emits `jvmdg$cast`/`jvmdg$isInstance` → `$Wrap` (real-I object) — designed path, corpus-covered |

**Identity divergence (semantic, documented-not-fixed):** the
checkcast/instanceof path produces a `$Wrap` while arg/return/field/merge flows
pass the raw member — the same member object is sometimes a `$Wrap`, sometimes
raw. `==`, `IdentityHashMap`, `synchronized`, `equals` diverge depending on the
flow path. Inherent to the wrap model.

**Fix candidate (ranked below):** an Analyzer-typed `aastore` interception —
when the array's component type is a gained `I` and the pushed value's static
type is a member `T`, insert `jvmdg$cast` before the store (real-I passthrough
on new targets, `$Wrap` on old). Rare site shape; needs frame analysis, same
machinery `remapClassCalls` already uses.

## M-4B — `stubs.idx` vs `JdkSurface` coverage

**Mechanism:** `ApiCoverageChecker` (`java-api/src/coverage`) diffs adjacent
ct.sym surfaces per boundary and classifies each required member as
available / only-on-parent / missing — but **only counts**: per-member data is
discarded into `missing.txt`/`parentOnly.txt`/`unmatched.txt`. It never
consults `vp.gained`/`vp.gainedMembers` or `RetroTypes.GAINED_CALLS`, so
members runtime-covered through gained dispatch are mislabeled missing.

**Matrix plug-in:** DONE as described — `coverage/<ver>/coverage-matrix.txt`
per version (`module;owner;name;desc;kind=stub|parent|gained|missing|
knownMissing`; owner is internal-name form so `;`-splitting works), the
gained-covered check runs before `missingStubs.add` using
`vp.gained`/`vp.gainedMembers` + `getSupertypes` closure (memoized per
owner), and `src/coverage/resources/known-missing.txt` is the allowlist.
Companion lists `available.txt`/`gained.txt`/`knownMissing.txt` land next to
the existing `missing.txt`; the jar task picks all of them up free.

**Seed triage (JDK introduction version → status today):**

| Member | JDK | Status |
|---|---|---|
| `Thread.ofVirtual/ofPlatform/startVirtualThread/isVirtual` + `Thread.Builder` | 21 | **stubbed** — emulated via `FakeVirtualThread` (real platform thread, `isVirtual`=instanceof); carrier/pinning semantics absent |
| `Executors.newVirtualThreadPerTaskExecutor` / `newThreadPerTaskExecutor` | 21 | **stubbed** — `ThreadPerTaskExecutor` (per-task factory thread, live-set shutdown contract); virtual variant on `ofVirtual().factory()` |
| `Thread.threadId()` | 19 | stubbed |
| `Math.clamp` ×4 | 21 | stubbed (+ StrictMath) |
| `CharSequence.isEmpty` / `.compare` | 15/11 | stubbed |
| `HexFormat` | 17 | stubbed (adapter, ~20 members) |
| `Stream.gather`/`Gatherer` | 24 | **missing** |
| `java.lang.foreign` (FFM) | 22 | **missing — near-unimplementable** (MemorySegment/Arena/linker surface) |
| `java.lang.classfile` | 24 | **missing** |
| `StructuredTaskScope` | preview→26 | **outside the surface** — preview classes are dropped by `JdkSurface.read`/`ApiCoverageChecker`, never even reports missing |
| `ScopedValue` | 25 | stubbed (15 members) |
| `System.Logger`/`LoggerFinder`/`Level` | 9 | stubbed (adapters + LoggerFinderImpl) |
| `Reference.reachabilityFence` | 9 | **missing** — trivial: `synchronized(ref){}` keeps alive |
| `Reference.refersTo` | 16 | stubbed |
| `Cleaner`/`Cleanable` | 9 | stubbed (adapter, PhantomReference impl) |
| `MethodHandles.privateLookupIn` | 9 | stubbed (emulates module-open refusal) |
| MH loop combinators (`loop`/`whileLoop`/`countedLoop`/`iteratedLoop`/`tryFinally`) | 9 | **missing** — implementable as MH composition |
| `Lookup.dropLookupMode` | 14 | **missing** — same `LookupCtor` trick as privateLookupIn minus modes |
| `Lookup.defineHiddenClass`/`ClassOption`/`ensureInitialized` | 15 | stubbed (`requiresRuntime`, HiddenClassLoader + runtime downgrade) |
| `CompletableFuture` j9 set (orTimeout, completeOnTimeout, completeAsync, copy, minimalCompletionStage, delayedExecutor, completedStage, failedFuture/Stage, newIncompleteFuture, defaultExecutor) | 9 | stubbed (timeouts via runAsync+sleep — correct but a thread per timeout) |
| `java.net.http.HttpClient` | 11 | stubbed (adapter — HTTP/1.1 via HttpURLConnection only; `version()`/`authenticator()`/`expectContinue()` no-ops) |
| `HttpClient` j21 AutoCloseable members | 21 | stubbed (no-ops) + real gained row `HttpClient→AutoCloseable`@65 — the case the checker mislabels |
| `HttpRequest`/`HttpResponse`/`HttpHeaders` | 11–25 | stubbed (adapters) |

Reporting ratchet proposal: `coverage-matrix.txt` + a `known-missing` allowlist
file (like `stubparity-known-diffs.txt`) so "deliberately skipped" ≠ "missing".

## M-4C — runtime semantics (non-member behaviors)

| Topic | Today's answer | Verdict |
|---|---|---|
| `java.version` / `Runtime.version()` | pass-through of the **real runtime** — `Runtime.version()` parses `java.runtime.version` of the target JVM | **correct-by-design** — feature-detection sees true target capability; document as policy |
| Virtual threads | `ofVirtual`/`ofPlatform`/`startVirtualThread`/`isVirtual`/`Thread.Builder` emulated via `FakeVirtualThread` (real daemon platform thread); `newVirtualThreadPerTaskExecutor`/`newThreadPerTaskExecutor` via `ThreadPerTaskExecutor` (factory thread per task) | platform-thread emulation is **defensible-doc** (no carrier scheduling, no pinning semantics, visible to ThreadMXBean, wrong priority group) |
| Wrap identity (`==`/`sync`/`IdentityHashMap`/`equals`) | generated `$Wrap` now delegates `equals`/`hashCode`/`toString` to `t` with same-wrap unwrapping; `==`/monitor/identity-map still `Object` identity; hand-written impl wrappers already delegated (`ReverseMap`, `UnmodifiableSequencedSet`, `ReverseList`) | identity divergence inherent (**defensible-doc**) |
| MissingStubError vs NSME/AME/NCDFE | `MissingStubError extends Error` from stub bodies + dispatch misses + `--noIndy` throw handles; no parity doc; unregistered members keep native sites → real NSME | **defensible-doc** — probing idioms catching `NoSuchMethodError` won't catch MissingStubError; document the parity policy |
| Stacktraces | `StackWalker` filters jvmdg frames; `Throwable.getStackTrace` does NOT → internals leak | **defensible-doc** — optionally extend filtering |
| `Class.getInterfaces`/`getGenericInterfaces` | authored view synthesized (filter `$Ov$`/`$Marker`, gained stand-ins via caller loader) | correct-by-design |
| `Class.getMethods`/`getDeclaredMethods` | pass-through real-only (documented honest-miss) | honest-miss by design |
| `getName`/`getModule`/`getClassLoader` on wrapped/adapted objects | reports impl/adapter class + DWCL | **defensible-doc** |
| Bootstrap-loaded classes | appended api jar to boot classpath loads **before** transformers → untransformed, NCDFE/NSME with no diagnostic | **under-documented gap** — document; a jar-index pre-scan could warn |
| ServiceLoader merge under shading | input-first ordering, per-line dedup, pruned-provider drop | correct-by-design (minor: `mergeServices` drops comments `mapServiceEntry` keeps) |
| Signed jars | `.SF/.RSA/.DSA/.EC` + per-name manifest digests handled correctly **except `Sealed:` survives** → sealed-package violation; `isSignatureFile` case-sensitive | **BUG** — strip `Sealed` main+entry attrs when signatures were dropped; case-insensitive suffix match |
| Serialization | lambdas: full SerializedLambda machinery; adapters/wraps: honest-miss (not Serializable); `RandomWrapper.writeObject(ObjectInputStream)` — **wrong param type, dead code** | **BUG** (writeObject signature typo — wrapper serializes `rgen` instead of throwing `NotSerializableException` like JDK) |
| Unsafe/natives | Unsafe members honest-miss (no stubs); VarHandle reimplemented on `sun.misc.Unsafe` | defensible-doc — no policy doc exists |
| GC/JMX/management | `RuntimeMXBean.getPid` parses `bean.getName()` up to `@` — NFE on exotic VMs; ThreadMXBean sees `FakeVirtualThread` (real vthreads are invisible); bean-surface placeholders noted | defensible-doc + getPid fragility |

### Robustness carry-overs (for the fix list)

- `--noIndy` member path `DispatchRuntimeDesugar→dispatchStub` pays uncached
  `ownMember`+`resolveImpl` per call — **113ns–22µs/op** (JMH, megamorphic on j8).
- `dispatchStub` swallows `RuntimeException` from `setAccessible` → silently
  falls back to the impl, masking real access bugs (`DispatchRuntime.java:171`).
- `RetroTypes` `IMPL_CTORS`/`DISPATCHERS`/`MEMBER_CLASSES` are strong-keyed
  `ClassLoader` maps → classloader leak under `DowngradingClassLoader`.
- `RetroTypes.loadResources` reads via its own loader — TCCL-only resources missed.
- `DowngradingClassLoader.transformClass` eagerly defines extras — one bad
  extra aborts the whole load; extras bypass parent-delegation.
- `J_L_StackWalker.walk` class-context offset math — AIOOBE risk, no bounds guard.
- `FakeVirtualThread`: MAX_PRIORITY group (real vthreads pin NORM), group captured
  from the first-touching thread, throwaway `new Thread()` for name generation.
- `BuilderImpl.name(prefix,start)` lazy-supplier ordering subtlety.
- `MissingStubError.create*` allocates a stack capture per call.
- `Bootstrap.premain` cache-cleanup ignores `listFiles` null + `delete` failures.

## Ranked fix list (P6 input)

1. **`Sealed:` manifest attr not stripped** — **FIXED**: `ManifestStep`
   drops `Sealed` from main+entry attrs when signatures were stripped (via
   `StepContext.data` + deferred manifest emission in `endFused`), and
   `ManifestUtils.isSignatureFile` is case-insensitive. Tests in
   `TestManifestUtils` + `TestFusedJarRun` (`unsignedSealedJarKeepsSeal`,
   `signedSealedJarLosesSealedAttrs`, `sealedAttributesStripped`).
2. **`RandomWrapper.writeObject` signature typo** — **FIXED**: param type
   `ObjectInputStream`→`ObjectOutputStream` (`j19/.../J_U_Random.java`) —
   now throws `NotSerializableException` as intended.
3. **`Executors.newVirtualThreadPerTaskExecutor`** — **FIXED**: stubbed as
   `ThreadPerTaskExecutor` (new `j21/impl` class, `AbstractExecutorService`
   spawning a factory thread per task, live-thread tracking for the
   shutdown contract); `newThreadPerTaskExecutor` came free. Virtual
   variant uses `ofVirtual().factory()` — the documented platform-thread
   emulation carries over (row above's defensible-doc caveats apply).
   `MissingStubError`-over-NSME for unregistered-member sites remains an
   open idea under item 10's parity-policy doc.
4. **`$Wrap` equals/hashCode/toString delegation** — **FIXED**: the wrap
   emits dedicated overrides delegating to `t` (same-wrap args unwrap
   first, else `t.equals(o)` — two wraps of one member compare equal).
   These replace — not duplicate — member-table entries for interfaces
   that redeclare them (List/Set), which previously routed
   `t.equals(otherWrap)` → always-false. `==`/`sync`/`IdentityHashMap`
   and the member's own `.equals(wrap)` stay divergent — inherent to a
   one-sided wrapper.
5. **`aastore` into gained-I arrays** — **FIXED**: `remapClassCalls` runs a
   second `Analyzer` pass (`TypedRefInterpreter` — a `BasicInterpreter`
   that keeps concrete ref types, since stock collapses them to
   `REFERENCE_VALUE`) typing the array operand; a scalar
   `I[]` store routes the value through the new dispatcher method
   `jvmdg$aastore` (member → `$Wrap`, null/I passthrough, genuine mismatch
   → `ArrayStoreException` — the real failure, not `CCE`). Multi-dim
   arrays skip (an `I[]` value can't be a member). Corpus:
   `Interfaces.gainedInterfaceArrayStore` + unit cases in
   `TestStructuralRemapTransform`.
6. **`Reference.reachabilityFence` / `Lookup.dropLookupMode` / MH loop
   combinators** — cheap honest stubs.
7. **`coverage-matrix.txt` report** — DONE. `ApiCoverageChecker` now emits
   per-version `coverage/<ver>/coverage-matrix.txt`
   (`module;owner;name;desc;kind=stub|parent|gained|missing|knownMissing`),
   plus `available.txt`/`gained.txt`/`knownMissing.txt`; gained-covered is
   classified via `vp.gained`/`vp.gainedMembers` against the member owner's
   source-version supertype closure; `known-missing.txt` (coverage
   resources) is the honest-gap allowlist. All files ship under
   `META-INF/coverage` in the api jar.
8. **`--noIndy` member-path caching** — `dispatchStub` reflective resolution
   cache keyed (site member, receiver class); 113ns–22µs → ~tens of ns.
9. **`dispatchStub` setAccessible swallow** — surface the access failure
   (warn/log) instead of silent impl fallback.
10. **Docs**: VT emulation limits, `Sealed`/bootstrap-load gaps, MissingStubError
    parity policy, wrap identity, getPid fragility — fold into
    `docs/Configuration.md` or a behavior doc.
11. **Unimplementable tier** (FFM, classfile, StructuredTaskScope,
    `Stream.gather`) — declare honest-missing policy + doc entries.
12. Classloader-leak keys in `RetroTypes` (WeakHashMap or per-loader `ClassValue`).

Deferred/not-gaps: `java.version` policy (real runtime, correct-by-design),
ServiceLoader merge, gained-interface arg/return/field flows (verifier
leniency — legal), `X.class` query sites (dispatcher, data-backed).
