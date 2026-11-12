# Bytecode Manipulation

This document describes the way in which JvmDowngrader manipulates the bytecode of Java class files.

## Overview

JvmDowngrader starts by initializing
a [VersionProvider](/src/main/java/xyz/wagyourtail/jvmdg/version/VersionProvider.java) which is used to provide a
downgrade between 2 sequential java versions.
This process is then chained until the desired version is reached.

The VersionProvider loads "Stubs", "Adapter" and "Modify" transforms which are used in the process of downgrading the
bytecode.
The "Stubs" are simple method replacements, that replace either a method call or a field access with a call to a static
method.
The "Modify" transforms allow for more complex changes to the bytecode, such as replacing InvokeDynamic calls with a
newly constructed private
method in the target class.
The "Adapter" transform replaces all class references with the target class.

## Pipeline architecture (3.0)

As of 3.0 the per-provider `VersionProvider.downgrade` chain runs inside a
version-agnostic engine ([`pipeline/`](/src/main/java/xyz/wagyourtail/jvmdg/pipeline/)).
The engine knows nothing about Java versions — it executes an ordered list of
`Stage`s over a name-keyed `TransformWorklist` of `ClassNode`s:

- Each `Stage` carries `ClassTransform`s applied to every class, optional
  `onEnter`/`onExit` `StageHook`s (whole-set barriers), a `precondition`
  (single-version runs), a `filter` (union runs — a class only passes through
  stages whose provider input-version it currently matches), and
  `captureLabels` that snapshot deep copies into `TransformContext.variants`
  for multi-release emission.
- [`DowngradeRegistrar`](/src/main/java/xyz/wagyourtail/jvmdg/version/DowngradeRegistrar.java)
  emits the stage list: one stage per provider boundary, decomposed into
  [`version/steps/`](/src/main/java/xyz/wagyourtail/jvmdg/version/steps/) —
  `ProviderInit` (ensureInit + version stamp + module-info requires) →
  `StubClasses` → `StubMethods` → `InsertAbstracts` → `OtherTransforms` →
  `FlushWarnings`. A provider that overrides `downgrade` wholesale keeps its
  own `ProviderStep` instead.
- Registered `@Stub`/`@Modify`/`@Adapter` content is scanned once per provider
  by [`StubScanner`](/src/main/java/xyz/wagyourtail/jvmdg/version/StubScanner.java)
  into a [`StubRegistry`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/StubRegistry.java)
  (per-owner store, frozen at stage entry) and applied by
  [`MemberStubTransform`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/MemberStubTransform.java)
  /[`StructuralRemapTransform`](/src/main/java/xyz/wagyourtail/jvmdg/version/map/StructuralRemapTransform.java).
  Hierarchy queries resolve through the memoized
  [`ClasspathIndex`](/src/main/java/xyz/wagyourtail/jvmdg/asm/index/ClasspathIndex.java)
  instead of re-parsing class bytes per lookup.
- Extras (`jvmdg$…` helper classes) produced by transforms are applied by
  `ExtrasApplyTransform` and rejoin the worklist for downstream stages.
- Jar runs traverse entries once through a `JarStep` chain in
  [`compile/jar/`](/src/main/java/xyz/wagyourtail/jvmdg/compile/jar/)
  (`SignatureStrip → Manifest → MultiReleaseRoute → ClassDowngrade`, or
  `PipelineJarStep` for the fused whole-set downgrade+shade), emitting via a
  sorted, timestamp-pinned `JarEntrySink`.
- Shading is a second registrar ([`ShadeRegistrar`](/src/main/java/xyz/wagyourtail/jvmdg/compile/shade/ShadeRegistrar.java)):
  `scan` indexes post-downgrade references into a
  [`ReferenceIndex`](/src/main/java/xyz/wagyourtail/jvmdg/compile/shade/ReferenceIndex.java),
  a closure `StageHook` barrier computes the reachable api set + inline
  candidates + per-slot prefix remappers + emitted api classes, then
  `inline`/`prefix`/`module-info` stages apply them.

Two engine invariants matter to every transform author:

- **No `COMPUTE_FRAMES`.** Transforms hand-author `FrameNode`s; writers use
  `COMPUTE_MAXS`. The maintained frames are what later analysis stages rely
  on.
- **Determinism.** Names of synthesized classes/members are pure functions of
  (class, site); output is a sorted write with pinned timestamps.

## Version Provider

Classes can be added for processing in the VersionProvider's init method by calling `stub(Class<?>)` this will parse
them for
`@Stub`, `@DefaultImpl`, `@Modify`, `@Adapter` and the `@InitRewrite`/`@InitArg`/`@InitPre`/`@InitPost`/`@InitFactory`
constructor annotations and add them to the appropriate lists.

Additionally, the version provider allows for other wider scale transforms that need to be done in order to downgrade
the bytecode by extending the
`otherTransforms` method.

### Stubs

To create a stub, a method must be `public static` and annotated with [`@Stub`](/src/shared/java/xyz/wagyourtail/jvmdg/version/Stub.java).
the `@Stub` annotation by default will use the first argument of the method as the target class and treat it as a
instance method replacement.
If the method is static, the `@Stub` annotation must be provided with an `@Ref` argument that specifies the target
class, this will treat the method as a static method replacement.

In some rare circumstances the target class should be different from the first argument, in that case (or when any
argument is needed to have it's type changed), the argument
may be annotated with `@Coerce` which will change the type of the argument to the specified type.

Stubs have several other fields that change their behavior:

* `abstractDefault` indicates if a method was abstract in the target version but now has a default implementation,
  this is used to determine if classes extending the target class need to also insert a reference to the stub.
  references to this method will not be replaced with the stub, and are handled specially as described below.
* `requiresRuntime` indicates if the stub requires the JVMDowngrader runtime in order to work as intended,
  mostly for things like reflection and runtime class definition.
* `noSpecial` indicates that the stub should not be used for `INVOKESPECIAL` calls.
* `downgradeVersion` adds an extra argument to the stub for the original version of the class, this is useful for
  multi-version stubs, such as reflection.
* `excludeChild` prevent a stub from being applied to a child-class of the target. for example, `String#isEmpty` is
  since java 6, but `CharSequence#isEmpty` is since java 15.

### Default implementations

[`@DefaultImpl`](/src/shared/java/xyz/wagyourtail/jvmdg/version/DefaultImpl.java) covers the `abstractDefault` case on
its own annotation: the JDK added a `default` body (or a formerly abstract member gained one in a later release),
but implementors compiled for the older shape lack it. The stub is emitted into each implementor that needs it as a
real `public` method; `invokeinterface`/`invokevirtual` callsites stay byte-identical and resolve to the inserted
bridge at runtime, and only `invokespecial` (`Interface.super.m()`) rewrites to the stub `invokestatic`.

### Constructor rewrites

Constructor handling lives on a dedicated annotation family registered into a per-owner `ctor` map — `@Stub` is
rejected on `<init>`. All of it is verifier-gated: `NEW`/`DUP` allocation sites and `this()`/`super()` delegation
calls are proven before surgery; shapes that can't be proven are left unchanged with a warning rather than
producing broken bytecode. `X::new` method references (`H_NEWINVOKESPECIAL` impl handles in `invokedynamic`
bootstrap args) route through the same map — a `jvmdg$handle$` static wrapper replays the rewrite or calls the
factory so constructor method-refs downgrade correctly.

* [`@InitRewrite`](/src/shared/java/xyz/wagyourtail/jvmdg/version/InitRewrite.java) — class-level (repeatable via
  `@InitRewrites`, which is also the explicit boxing for source sets below Java 8) declares one rewrite:
  `ref` is the original `<init>` ref, `to` the descriptor of the replacement constructor of the same class, and
  `keep` maps each `to` argument to an original argument index or a converter-produced slot (`-1` = converter).
* [`@InitArg`](/src/shared/java/xyz/wagyourtail/jvmdg/version/InitArg.java) — method-level (repeatable): a
  `public static` converter producing one `to` argument from original args (`from`, `target`/`owner` disambiguate
  overloads and the `keep` slot via `slot`).
* [`@InitPre`](/src/shared/java/xyz/wagyourtail/jvmdg/version/InitPre.java) — method-level (repeatable): runs
  before the retargeted `<init>` (e.g. range checks) from original args.
* [`@InitPost`](/src/shared/java/xyz/wagyourtail/jvmdg/version/InitPost.java) — method-level (repeatable): a tail
  invoked after the retargeted constructor — signature `(ConstructedType, keptArgs…)` — e.g. `initCause` chaining.
* [`@InitFactory`](/src/shared/java/xyz/wagyourtail/jvmdg/version/InitFactory.java) — replaces construction
  outright: `new X(args)` becomes `X stub(args)` (the method must be `public static` and return the constructed
  object). Delegation shapes are always left unchanged — an uninitialised `this` can't be passed out.

See `J_L_IndexOutOfBoundsException` (j9/j16), `J_U_NoSuchElementException`, `J_I_FileReader`/`J_I_FileWriter`,
`J_U_BigInteger`, and the j10 `J_I_PrintStream`/`J_U_Formatter` charset overloads for examples.

### Modify

The `@Modify` annotation is used to replace a specific instruction with a new instruction or list of instructions.
The `@Modify` annotation is provided with a `@Ref` argument that specifies the target class, method and descriptor.
the method must be `public static`; its parameters are a subsequence of
`MODIFY_SIG` — `MethodNode mnode, int i, ClassNode cnode, Set<ClassNode> extra, boolean noSynthetic`
— declared in order (a handler may take just `(mnode, i)`, or all five; the
`noSynthetic` flag mirrors the debug flag for generated members).

This is used to replace `INVOKEDYNAMIC` calls and other multi-insn rewrites the declarative annotations can't
express. Constructors no longer use `@Modify` — the `@InitRewrite`/`@InitFactory` family above covers them.

### Adapter

The `@Adapter` annotation is used to replace all references to a class with a different class.
By default, it replaces the `value` class with the one it's annotated on, but it can be provided with a `target` class.
there is also a `keepInterface` field, if the stub is an interface, it can optionally be completely removed from classes
referencing it.

### Choosing a stub form

| Need | Use |
|---|---|
| Replace a call/field access with a static helper | `@Stub` (default choice) |
| Emit a fallback for a method that's `default`-only on newer Java | `@DefaultImpl` |
| Replace `new X(args)` with `X stub(args)` (factory; can't apply to `this()`/`super()` delegation) | `@InitFactory` |
| Retarget `<init>` to another ctor of the same class | `@InitRewrite` (+ `@InitArg`/`@InitPre`/`@InitPost`) |
| Replace an instruction sequence (indy, multi-insn rewrite) | `@Modify` |
| Redirect every reference to a class type | `@Adapter` |
| Make a stub argument/return type differ from the target's | `@Coerce` on the parameter |
| Tag a stub with the JEP it implements (enables `debug --skipJEP`) | `@JEP(<n>)` on the member |
| Pull a non-class resource into shaded output when a member is used | `@RequiresResource("path")` |
| Declare reflection-only references the index should see | `@ReflectionReferences({@Ref(...)})` |
| Keep a jvmdg-internal public member on a stub/adapter class out of the `unmatched.txt` coverage ratchet | `@Internal` (`@CoverageIgnore` is a deprecated alias) |

`@JEP` values are the JEP number that introduced the member — the audit test
cross-checks them against the provider's JEP table, so an untagged newer-API
stub fails the suite rather than silently skipping `--skipJEP` filtering.

`@Internal` is only for public members that genuinely don't mirror a JDK
member (internal ctors, helper defaults, adapter record components). Members
that *do* mirror the JDK are excluded from `unmatched.txt` automatically:
the checker resolves each registered site against the target surface and
each adapter member against its own version's surface, including inherited
declarations and covariant returns — so BEHAVIOR stubs and covariant
redeclarations need no marker at all. Putting `@Internal` (or
`@CoverageIgnore`) on a `@Stub`/`@Modify` member fails registration.

### RequiresResource

This annotation is a hint to the Shading Api task that the class or method requires a resource from the api jar in order
to operate.
such as emoji data in the java 21 `Character` class.

### InvokeDynamic Method Insertion

If a stub does not exactly match the method descriptor of an invokedynamic method it is replacing, it may be necessary
to wrap it with another method to adapt the types.
This is done automatically while stubbing handles in InvokeDynamic arguments.

### Abstract Method Insertion

The providers also scan the classpath to determine the inheritance hierarchy of the target class.
This is then used to insert abstractDefault stubs into classes that extend the target class of those stubs that do not
already implement the stubbed method themselves, or in a super class.

### Other Transforms

The `otherTransforms` method is used to apply wider scale transforms to the bytecode, such as replacing all references
to a class with a different class. It is an overload chain on `VersionProvider`
— the fullest form receives
`(clazz, extra, getReadOnly, warnings, enableRuntime, memberResolver, superTypeResolver)`
and each narrower overload delegates down to `otherTransforms(clazz)`; override
the narrowest that has the context you need.
the current list of transforms is:
* Java 25->24
  * Create static main methods to call instance main methods.
* Java 17->16
  * Remove `ACC_SEALED` and add `@PermittedSubClasses` annotation
* Java 16->15
  * Remove `ACC_RECORD` and add `@RecordComponents` annotation
* Java 15->14
  * Insert synthetic "bridge" methods for private access from handles in nestmates.
* Java 11->10
  * Add a `@NestHost` annotation to nest members
  * Add a `@NestMembers` annotation to nest hosts
  * Insert synthetic "bridge" methods so that private fields/methods can be called from nest members.
  * Replace Constant Dynamic calls with `INVOKEDYNAMIC`
* Java 9->8
  * Make private interface methods use `INVOKESPECIAL` instead of `INVOKEINTERFACE`.
* Java 8->7
  * Move private/default methods in interfaces to a `$jvmdg$StaticDefaults` inner class.
* Java 7->6
  * replace INDY method calls with some *fun* `MethodHandle` stuff.
* Java 6->5 
  * remove frames
