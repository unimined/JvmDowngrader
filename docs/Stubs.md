# Writing Stubs

This guide is the practical companion to [docs/Bytecode Manipulation.md](/docs/Bytecode%20Manipulation.md):
it explains what a JvmDowngrader stub *is*, where stubs live, every annotation
an author can use, how a stub travels from source to emitted callsite, and how
to test and debug one. It targets first-time stub authors and reviewers of stub
PRs alike. When in doubt, the annotations under
[`src/shared/java/xyz/wagyourtail/jvmdg/version/`](/src/shared/java/xyz/wagyourtail/jvmdg/version/)
are the ground truth — every field below is verified against them.

## What a stub is

A stub is replacement Java code that stands in for an API that does not exist
(or misbehaves) on an older JVM. When JvmDowngrader rewrites a class file from
class-version `N` to an older target, calls to missing members are redirected
to stub implementations shipped in the `java-api` jar — which is itself
compiled at the toolchain version and then self-downgraded so it can run on
the very JVMs it patches.

The goal is **behavioral parity**, not just a matching signature. A stub that
"works" but throws the wrong exception, iterates in the wrong order, or
accepts arguments the real API rejects is a bug. Stubs are compared
line-for-line against the real JDK by the parity corpus (see
[Testing stubs](#testing-stubs)).

> **Never copy JDK source.** OpenJDK sources are GPL-with-classpath; copying
> them would contaminate this project's license. Write every stub body from
> the API specification (JLS/Javadoc/behavioral contract) and from observed
> behavior — not from `java.base` sources. Reviewers: flag any stub body that
> looks transliterated.

### The three axes

Every registered entry sits on three axes:

| Axis | Values | Where it is decided |
|------|--------|---------------------|
| **Kind** — *why* it exists | `MISSING_API`, `BEHAVIOR`, `LANGUAGE` (`StubKind`, plus `AUTO`) | `kind=` on the annotation; `AUTO` derives from `memberSince` vs. the target version |
| **Scope** — *what* it replaces | a single **member** (method/field/ctor) or a whole **class/interface** | member annotations (`@Stub`, ctor family, `@DefaultImpl`, `@Modify`, `@IndyDesugar`) vs. `@Adapter` |
| **Mechanism** — *how* it applies | callsite rewrite, mechanical type remap, adapter wrapping, gained-member binding | the annotation used |

Kinds matter because `Stub-Policy` filters on them: `MISSING_API` fills a gap
the old JVM genuinely lacks; `BEHAVIOR` shadows a member that exists but
misbehaves; `LANGUAGE` desugars bytecode shapes (indy bootstraps, string
concat) rather than APIs.

## Where stubs live

Stub source lives in the `java-api` project, one **versioned source set per
provider boundary**:

```
java-api/src/java9/java/xyz/wagyourtail/jvmdg/j9/
java-api/src/java10/java/xyz/wagyourtail/jvmdg/j10/
…
java-api/src/java21/java/xyz/wagyourtail/jvmdg/j21/
```

Each `j<N>` package carries `stub/<module>/` classes (the annotated surface)
and `impl/` siblings (plain helper classes with no annotations). A stub only
applies when the input is at or above its provider boundary — a `java9` stub
fires for 9+ inputs downgraded below 9, never for a `java17`-only member.

**Naming convention:** `J_` + package-initial segments + `_` + class name —
`J_L_Integer` is `java.lang.Integer`'s stub class, `J_U_SequencedCollection`
is `java.util.SequencedCollection`, `J_I_InputStream` is `java.io.InputStream`,
`J_T_LocalDate` is `java.time.LocalDate`, `J_U_R_RandomGenerator` is
`java.util.random.RandomGenerator`, `J_L_I_StringConcatFactory` is
`java.lang.invoke.StringConcatFactory`. Nested types become nested stub types.

**Shared runtime helpers** live under
[`src/shared/java/xyz/wagyourtail/jvmdg/`](/src/shared/java/xyz/wagyourtail/jvmdg/):
the annotation vocabulary in `version/` and the emitted-site runtime in
`stub/` (`RetroTypes`, `DispatchRuntime`, `GainedRuntime`, `ReflectionTable`,
`MissingStubError`, `PartialStubError`). `src/shared` must stay **ASM-free and
dependency-free** and pass the Java 8 source gate — it ships inside the
downgraded jar and runs on target JVMs.

## Lifecycle: source → registry → emitted callsite

1. **Author** a `public static` handler (or adapter/constructor annotation) in
   the right `java<N>` source set.
2. **Registration** — `StubScanner` reflectively scans each provider's stub
   classes (recursing into nested classes) and fills a per-provider
   `StubRegistry`, keyed by internal owner name.
3. **Indexing** — `StubIndexWriter` (`genStubIndex` task in
   `java-api/build.gradle.kts`) runs the *same* scan at api-jar build time,
   stamps `memberSince` from the `ct.sym` JDK-surface index, derives
   `excludeChild`/retro/gained tables, and serializes to
   `META-INF/jvmdg/stubs.idx` plus flat resources (`retrotypes`, `excludes`,
   `wraps`, `gained`, `gained-calls`, `reflection.idx`).
4. **Loading** — `ensureInit` loads the index; `jvmdg.debug.noStubIdx` forces
   the reflective-scan path instead. Both paths must produce equivalent
   registries — `DerivedExclusions`/`DerivedGained` re-apply the flattened
   resources so scan and idx resolve identically.
5. **Emission** — `StubMethods` runs `StructuralRemapTransform` (type-slot
   remap, retro-supertype/gained rewrites) then `MemberStubTransform`
   (indy desugars, `@Modify`, member stub rewrites, ctor transforms),
   then `InsertAbstracts` (gained/abstract bridges).
6. **Runtime dispatch** — emitted sites resolve through `DispatchRuntime`,
   `GainedRuntime`, or generated `…$jvmdg$retro`/`gen/gained/` helpers.

## Annotation reference

All annotations live in
[`xyz/wagyourtail/jvmdg/version/`](/src/shared/java/xyz/wagyourtail/jvmdg/version/)
and are `RUNTIME`-retained so `StubScanner` can see them.

### `@Stub`

Marks a `public static` method as the replacement for a target member;
matching callsites are rewritten to call it. A `<init>` `@Stub` is a
registration error — use the constructor family below.

| Field | Default | Meaning |
|-------|---------|---------|
| `ref()` | `@Ref()` | Target owner/member/desc when inference can't resolve it (see below). |
| `downgradeVersion()` | `false` | Push the provider's input version as a trailing `int` argument. |
| `callerClass()` | `false` | Push `ldc` of the callsite's containing `Class` first — for caller-sensitive APIs like `Class.forName` so resolution uses the caller's loader. Emitted before `downgradeVersion`'s int. |
| `requiresRuntime()` | `false` | Equivalent to `condition = REQUIRES_RUNTIME`; combining it with a non-`ALWAYS` `condition` is a registration error. |
| `condition()` | `ALWAYS` | Activation gate; `PREVIEW` entries register but only fire under `--allowPreview`. |
| `noSpecial()` | `false` | Never rewrite `INVOKESPECIAL`/`NEWINVOKESPECIAL` sites — `super.m()` keeps native semantics. Auto-derived for `OWN_INTERFACE` adapter members; declare it manually only for members whose owner isn't class-stubbed. |
| `excludeChild()` | `{}` | Owner names for which this inherited stub must not fire. |
| `kind()` | `AUTO` | Force the `StubKind` when `ct.sym` presence can't express intent. |

**Signature inference.** With no `ref`, the first parameter is the receiver
and the remaining parameters + return type form the member descriptor —
`readAllBytes(InputStream in)` registers `readAllBytes;()[B` under
`java/io/InputStream`. With `ref`, the `@Ref` triple pins the registered
member directly; use it for behavior stubs
([`J_L_Integer.parseInt`](/java-api/src/java9/java/xyz/wagyourtail/jvmdg/j9/stub/java_base/J_L_Integer.java))
and whenever the authored signature uses the remap target rather than the
real member type
([`J_U_Collections.unmodifiableSequencedCollection`](/java-api/src/java21/java/xyz/wagyourtail/jvmdg/j21/stub/java_base/J_U_Collections.java)).

**Adapter/`@Coerce`-typed parameters and returns register as the real JDK
types** (`StubHandler.coercedParamTypes`) — the registered descriptor never
contains `J_*` stand-ins. This is why `@StubTest` claims must use real JDK
types.

**Field stubs** use the same annotation: a non-`void` return registers a
getter, a `void` return a setter —
[`J_T_LocalDate.EPOCH`](/java-api/src/java9/java/xyz/wagyourtail/jvmdg/j9/stub/java_base/J_T_LocalDate.java)
registers the `EPOCH` field via an explicit `ref`.

### Constructor stubs — `@InitRewrite`, `@InitArg`, `@InitPre`, `@InitPost`, `@InitFactory`

Constructor replacement has its own family (all repeatable via their `…s`
containers):

- `@InitRewrite(ref = …, to = …, keep = {}, downgradeVersion, requiresRuntime,
  condition)` — retarget `new X(args)` to another constructor `to` of the same
  class; `keep` retains original arguments.
- `@InitArg(target = …, slot = …, from = {}, owner = "")` — supply a
  synthesized argument at `slot`; `from`/`owner` bind the supplier.
- `@InitPre` / `@InitPost(target = …, from = {}, owner = "")` — pre- and
  post-construction hooks around a rewrite.
- `@InitFactory(ref = @Ref(), downgradeVersion, requiresRuntime, condition)` —
  replace the `new`+`<init>` pair with a static factory method.

See
[`J_L_IndexOutOfBoundsException`](/java-api/src/java16/java/xyz/wagyourtail/jvmdg/j16/stub/java_base/J_L_IndexOutOfBoundsException.java):
`@InitRewrite` turns `(J)V` into `(String)V` and `@InitArg` synthesizes the
message.

### `@DefaultImpl`

A `public static` fallback body for an interface member: instead of rewriting
callsites, the implementation is materialized into implementors that lack the
member. Params/returns follow the same `@Coerce`/`@Adapter` registration
rules as `@Stub`.

### `@Adapter`

Declares that a stub type stands in for a JDK type in signatures — the core
of class-scope stubbing. Every `instanceof`/`checkcast`/member lookup on the
real type is routed through generated machinery.

| Field | Default | Meaning |
|-------|---------|---------|
| `value()` | — *(required)* | The real JDK type being stubbed, e.g. `"java/util/random/RandomGenerator"`. |
| `target()` | `""` | The type signatures are remapped **to**. Empty → the stub class itself (`OWN_INTERFACE`); an existing JDK supertype → `SUPERTYPE`. |
| `keepInterface()` | `true` | Keep the original interface in the type hierarchy. |
| `mode()` | `AUTO` | `SUPERTYPE` = remap to a real supertype; `OWN_INTERFACE` = remap to the stub type carrying default bodies. `AUTO` resolves from `target()`. |
| `implementedBy()` | `{}` | Classes that get the interface injected into their hierarchy. |
| `wrap()` | `""` | Member producing the runtime wrapper for `OWN_INTERFACE` adaptation. |
| `impls()` | `{}` | Concrete implementation classes; impls registered on a sub-adapter are flattened **upward** through the adapter graph so ancestors see them too. |
| `signaturePolymorphic()` | `{}` | Member names dispatched polymorphically (VarHandle-style access modes); member-stub misses on the owner emit the second indy contract via the adapter's `jvmdg$bootstrap` hook. |

Adapter entries are ordered by **adapter depth** and pulled up deterministically;
`$Marker` interfaces are generated so `instanceof` checks have a real type to
test. See [Writing an adapter](#writing-an-adapter) for the full model.

### `@Modify`

The escape hatch for call-site surgery that isn't a clean member redirect —
`ref()` (default `@Ref()`) pins the target member, `kind()` (default `AUTO`)
classifies it. The handler signature is built around `MethodNode`, the
instruction index, the containing `ClassNode`, an extra-class `Set`, a
synthetic flag, and an optional trailing `ModifyContext`. Reach for `@Modify`
only when `@Stub` can't express the transformation; `signaturePolymorphic`
dispatch and `@IndyDesugar` cover the common reasons.

### `@IndyDesugar`

`ref()` is required and names the *bootstrap* member of an `invokedynamic`
site. The handler — `public static void h(ModifyContext ctx)` — inspects
`ctx.indy` and replaces the instruction with ordinary bytecode.
[`J_L_I_StringConcatFactory`](/java-api/src/java9/java/xyz/wagyourtail/jvmdg/j9/stub/java_base/J_L_I_StringConcatFactory.java)
is the canonical example (`makeConcatWithConstants`), alongside
`J_L_R_ObjectMethods` and `J_L_R_SwitchBootstraps`.

> `@IndyDesugar` matches **indy sites whose bootstrap is the referenced
> member**. A *direct* `invokestatic` of that same bootstrap method is a
> different callsite — it is not intercepted (the `stubtests21` corpus keeps
> plain-`makeConcat` fixtures to prove both paths).

### `@Coerce`

Parameter-level: `value()` (`Class<?>`, required) substitutes the declared
param type at registration so the member binds/looks up against the real JDK
type (`StubHandler.coercedParamTypes`). It remains a standalone annotation —
not folded into anything — but today **no hand-written `java-api` stub uses
it**; generated member forwarders rely on the same shape
(`@Coerce(Adapter) Object self`). Use it when the Java-declared parameter type
cannot be the registered type.

### `@Internal` / `@CoverageIgnore`

Marks `public` methods/fields/ctors that exist for jvmdg's own machinery and
are **not** user-facing API. Placing `@Internal` on a method that already
carries a primary stub annotation (`@Stub`, `@Modify`, `@IndyDesugar`,
`@DefaultImpl`, the ctor family) is a registration error. `@CoverageIgnore`
is the deprecated alias.

### `@RequiresResource`

On a method or a stub type: `value()` (`String[]`) lists resources the stub
needs at runtime. The annotation is retained in the class file, and shading
pulls the listed resources in when the annotated element is part of the
reference closure. Put it on the **type** when several members share the
resource, on the **member** when only one path needs it — member-level is
carried with that member's entry.

### `@ReflectionReferences` and `@Ref`

`@ReflectionReferences` on a stub method declares `Ref[]` members the
*emitted* site will need — e.g. a member-stub indy declares its impl `Handle`
so the shading reference closure keeps it (descriptors are remapped through
the provider chain into stand-in vocabulary). `@Ref` itself is the
`{value: internal-owner, member: name, desc: descriptor}` triple used by
`@Stub.ref`, `@InitFactory.ref`, `@StubTest.member`, known-diff refs, and
`Include-Stubs` flags — always in registered (real-JDK-type) vocabulary.

### `@JEP`

Type- or method-level provenance tag carrying the JEP number a stub
implements. Informational; used in audits of API coverage.

### `StubKind` / `StubCondition`

Not annotations — the enums behind `kind=`/`condition=`:

- `StubKind`: `AUTO` (member stubs → `MISSING_API` if the member is absent at
  the target, else `BEHAVIOR`; `@Modify` entries → `LANGUAGE`), `MISSING_API`,
  `BEHAVIOR`, `LANGUAGE`.
- `StubCondition`: `ALWAYS`, `REQUIRES_RUNTIME` (fires only when the runtime
  transform path is available — jar-mode without the agent leaves the site
  native), `PREVIEW` (fires only under `--allowPreview`).

**Not annotations at all:** `Include-Stubs`/`Exclude-Stubs`/`Stub-Policy` are
scoped configuration (`--includeStub`, `-is`; see `Flags`), and `stubWithExtras`
is the worklist mechanism that applies extra classes a stub contributes — no
`@Included`/`@StubWithExtras` annotation exists.

## Registration rules worth knowing

- Handlers must be `public static` — `ClassMapping.addStub` rejects otherwise.
- One method, one primary annotation — multiple primaries are a registration
  error, reported per stub class.
- `ClassStubEntry` **bakes** the adapter's `itf` bit, `jvmdg$instanceof`/
  `jvmdg$checkcast` hooks, resolved mode, `implementedBy`, `wrap`, and
  signature-polymorphic metadata at registration and serializes them into the
  index — emitters must never re-derive them reflectively.
- `memberSince` is stamped from `ct.sym` at index time (it is side data, not
  part of entry identity); explicit `kind=` overrides are stored separately.
- Registered descriptors use **real JDK types**; adapter stand-ins in
  callsite descriptors are translated back before lookup (`unstubMember`).

## What the emitters produce

| Input site | Emitted shape |
|------------|---------------|
| `INVOKESTATIC` of a stubbed member | `invokestatic` to the handler |
| `INVOKEVIRTUAL`/`INVOKEINTERFACE` of a member stub | `invokedynamic` bootstrapped by `DispatchRuntime` — a `ClassValue` PIC that resolves the receiver's own member first (**user overrides win**), falling back to the registered impl |
| Member call on a *gained* interface | `invokedynamic` via `GainedRuntime` (name+desc, authored-version ceiling) |
| `instanceof`/`checkcast` on a retro-stubbed type | `invokestatic` to the generated `<stubType>$jvmdg$retro` dispatcher (`wrap`/`cast`/`isInstance`/array forms); gained supertype interfaces route to `gen/gained/<I>` structural helpers |
| `INVOKESPECIAL`/`NEWINVOKESPECIAL` of a member stub | rewritten to the impl by default; `noSpecial` keeps the native super-dispatch (auto-derived for `OWN_INTERFACE` adapter members) |
| Miss on a `signaturePolymorphic` owner | second indy via `DispatchSites.polymorphicInsn` → the adapter's `jvmdg$bootstrap` hook |
| Implementor missing an abstract/gained member | `InsertAbstracts` injects a public bridge that `invokestatic`s the **handler directly** — never the dispatcher (the bridge's own declaration would mark the class and recurse) |
| Type slots in descriptors/signatures | mechanical remap via `StructuralRemapTransform`; `wrap`/`impls` coercions via `RetroTypes`/`wrapImpl` |

## Worked example: `InputStream.readAllBytes`

From
[`java-api/.../j9/stub/java_base/J_I_InputStream.java`](/java-api/src/java9/java/xyz/wagyourtail/jvmdg/j9/stub/java_base/J_I_InputStream.java):

```java
public class J_I_InputStream {

    @Stub
    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
```

- **Registration:** no `ref` → owner = first param `java/io/InputStream`,
  member = `readAllBytes;()[B`. `ct.sym` says `readAllBytes` appeared in 9, so
  a `java9+ → ≤8` run classifies it `MISSING_API`.
- **Emitted site:** `in.readAllBytes()` compiles to
  `INVOKEVIRTUAL java/io/InputStream.readAllBytes ()[B`; after the member
  lookup hits, the site becomes an `invokedynamic` linked by
  `DispatchRuntime`. On an old JVM the PIC finds no override on the concrete
  stream and calls the static body above; on a JDK where `ByteArrayInputStream`
  (say) carries its own `readAllBytes`, that override wins — preserving real
  dispatch semantics instead of blanket-redirecting every call.

## Writing an adapter

Use `@Adapter` when a whole *type* is the thing that's missing.

- **`SUPERTYPE` mode** is simplest — the new JDK type extends an existing
  supertype, so signatures remap to the real supertype:
  [`J_U_SequencedCollection`](/java-api/src/java21/java/xyz/wagyourtail/jvmdg/j21/stub/java_base/J_U_SequencedCollection.java)
  declares `@Adapter(value = "java/util/SequencedCollection",
  target = "java/util/Collection")`. Member stubs on the adapter then bind
  against the target (`reversed(Collection<E> self)` registers under
  `java/util/Collection` in post-remap vocabulary).
- **`OWN_INTERFACE` mode** is needed when there is no suitable existing
  supertype — the stub interface itself carries `default` bodies and becomes
  the remap target. `wrap=` names the member that produces the runtime
  wrapper around raw JDK objects; `impls=` lists the concrete impl classes
  consulted in order — a sub-adapter's impls are flattened **upward** into
  its ancestors' impl sets, so wrapping at a parent level can still produce
  a more specialized impl.
- [`J_U_R_RandomGenerator`](/java-api/src/java17/java/xyz/wagyourtail/jvmdg/j17/stub/java_base/J_U_R_RandomGenerator.java)
  shows a whole adapter graph: the root adapter carries
  `impls = {BasicRandomGeneratorImpl.class}` and nested `@Adapter` interfaces
  mirror `RandomGenerator`'s sub-interfaces (`SplittableGenerator`,
  `JumpableGenerator`, …), each adding its own `impls` where needed.
- Generated `$Marker` interfaces give `instanceof` a real type to test;
  generated `…$jvmdg$retro` dispatchers do the `impls` instanceof/new chain
  under `catch (LinkageError)`, tailing to `RetroTypes.wrapImpl`.
- Ordering is adapter-depth + pull-up: process sub-adapters after the parents
  they extend, and put `impls` on the highest interface that logically owns
  them.

## Version gating and policy

- **Provider boundary:** the `java<N>` source set is the activation ceiling —
  a stub applies when the input is ≥ its boundary and the target is below the
  member's introduction version.
- **`memberSince`:** derived from `ct.sym` at `genStubIndex` — not authored.
  `AUTO` kind = `MISSING_API` when the member postdates the target,
  `BEHAVIOR` when it doesn't; override `kind=` when presence data misleads.
- **Conditions:** `requiresRuntime`/`REQUIRES_RUNTIME` gates on the runtime
  transform path; `PREVIEW` on `--allowPreview`.
- **Scoped config:** `Stub-Policy` = `FULL` (everything), `NO_BEHAVIOR` (skip
  behavior stubs), `MECHANICAL` (language desugars only — missing APIs are
  reported per `On-Missing-Api`). `Exclude-Stubs` scopes entries out;
  `Include-Stubs` (`--includeStub`/`-is`) re-includes them — both keyed by
  `FullyQualifiedMemberNameAndDesc`, the `index.txt` identity form.
- **Multi-release:** `META-INF/versions/<N>` entries route through
  `MultiReleaseRouteStep` so a stubbed jar can keep per-release variants.

## Testing stubs

The parity corpus lives in `:testing` (`./gradlew :testing:test`):

- **`@StubTest(member = @Ref(value, member, desc))`** on a
  `src/stubtests/java` probe claims coverage. The claim must use the
  **registered** descriptor — real JDK types, never `J_*` stand-ins (e.g.
  `()Ljava/util/SequencedCollection;` not the adapter's type). If generation
  reports a mismatch, check `build/reports/stubtests/untestable.txt` and
  `index.txt` for the canonical ref.
- **`untestable.txt` is a ratchet** — it must stay header-only; a member with
  no coverage path fails the suite.
- **`stubparity-known-diffs.txt`** format:
  `<slug|all> <stable-ref> ==> <native-glob> ==> <downgraded-glob>` under a
  required rationale comment. `*`-globs pin diff *content*; asserted
  bidirectionally — a new diff fails, a diff that starts matching fails
  (remove the entry), a changed diff shape fails (update it).
- **`deferred.txt`** lists registered members whose impls only activate below
  the tested matrix (the 8→7 scope) — legitimate non-coverage.
- **Feature tests** (`src/featuretests/java`, `@FeatureTest` +
  `Support.run`) probe end-to-end output; native vs. downgraded diffs
  line-for-line.
- Tests run JUnit 5 **parallel/concurrent** — anything mutating JVM-global
  state (system properties, singletons) needs `@Execution(SAME_THREAD)`.

## Debugging a stub

- `-Djvmdg.debug.noStubIdx` — force the reflective scan; use it to test
  whether a bug lives in index-vs-scan parity (both paths must behave
  identically).
- `-Djvmdg.debug.dumpClasses` — dump transformed classes; then
  `javap -c` the output and check the emitted site shape against the table
  above.
- `-Djvmdg.corpus.debugExc` — make corpus `EXC:` lines also print stack
  traces (the line protocol stays clean for diffs).
- `-Djvmdg.debug.noSynthetic`, `-Djvmdg.stubPolicy`, `-Djvmdg.includeStub` —
  narrow what's applied.
- `build/reports/stubtests/` — `index.txt`, `inventory.txt`, `coverage.txt`,
  `untestable.txt`, `deferred.txt` are the registry-side source of truth for
  "what registered under which descriptor".

## Common pitfalls

- **Emitters never reflect on stub classes.** `Class.forName` on an API class
  in the transform path can load a pre-downgrade class
  (`UnsupportedClassVersionError`) or miss synthetic test fixtures — that's why
  `ClassStubEntry.itf` and the hooks are baked at registration. New emitter
  code must consume baked fields only.
- **Authored vs. output ceilings:** gained/retro helpers carry the *calling*
  class's authored version as their ceiling — don't confuse the version the
  stub is written at with the version the output will run at.
- **Descriptor collapse:** after type remapping two overloads can collapse to
  one descriptor; the registry checks for collisions — if your stub "isn't
  found", diff the registered form in `index.txt` first.
- **`@IndyDesugar` ≠ call interception** — it matches indy bootstraps, not
  direct invokes of the bootstrap method.
- **`requiresRuntime` + explicit non-`ALWAYS` `condition`** is a registration
  error — they're the same flag.
- **Never emit dispatch machinery from gained-dispatch machinery** — bridges
  call handlers directly to avoid self-recursion.
- **`src/shared` rules:** no ASM, no dependencies, Java 8 only — a `var` or
  `List.of` in shared code breaks the jar on every target.
- **Test claims lie by descriptor:** `@StubTest` with the `J_*` source type
  silently claims nothing; the suite's `untestable.txt` ratchet will catch
  it at review, not at authoring.
