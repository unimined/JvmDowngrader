# commons-mirror strategy

`wagyourtail/commons` (`commons-core`, `commons-asm`, `commons-java7`) and
JvmDowngrader share several forked class lineages. They have diverged — commons
uses lombok, `commons.core` logging/utils, and synchronous `walkFileTree`;
jvmdg targets Java 7 with no lombok and `AsyncUtils.visitPathsAsync`. Ports
therefore *adapt* rather than copy, and fixes must travel both directions.

This document records which side each lineage now lives on, so a fix lands in
the right repo first.

## Direction of truth

| Code | Direction | Notes |
|---|---|---|
| `ASMUtils`, `AnnotationUtils` | commons-asm → jvmdg | jvmdg adopted the commons copy during the 3.0 overhaul (helper dedup, larger `AnnotationUtils`). |
| `ClassInfo`/`MethodInfo`/`FieldInfo` (`asm/info/`) | commons-asm → jvmdg | Adopted incl. `isInheritable`; the jvmdg `ClassInfo` fixes (classpath-index caching) fold back upstream later. |
| `MemberNameAndDesc`/`FullyQualifiedMemberNameAndDesc` | commons-asm → jvmdg | Adopted the `ignoreReturnValue` member-key design. |
| `pipeline/` engine + stub model | jvmdg → commons-asm (later) | `Stage`/`TransformPipeline`/worklist/`StageHook`/`FusibleTransform`, `StubRegistry`/`StubScanner`/`MemberResolver`/`TypeRemap`, `ClasspathIndex`/`NamespaceView`/`WorkingClasses`, `Lazy`/`Pair`. Port waits until the API settles here. |
| `compile/jar/` (`JarStep`/`FusedJarRun`/`JarEntrySink`) | jvmdg → commons-asm (later) | Port of unimined-remap's Kotlin `FusibleJarStep`; see divergence note below. |
| `compile/shade/` (`ReferenceIndex`/`RefExtractor`/`IndexScanner`) | jvmdg → commons-gradle | Minimal shading/tree-shaking substrate; `RefExtractor` is the plugin surface. **Replaces** commons-asm's `graph/ReferenceGraph` (same forked lineage, same weaknesses — no member-level edges, no versioned lookup). jvmdg's own `compile/shade/ReferenceGraph.java` was deleted in the rewrite. |
| `Utils`, `Lazy`, `Pair`, `*Enumeration` | jvmdg ⇄ commons-core | Independent forks; port fixes both directions. |
| `AsyncUtils` | jvmdg ⇄ commons-java7 | jvmdg's async tree-walk lives here; commons-java7 is the j7 counterpart. |
| `ResourceClassLoader` | jvmdg ⇄ commons-core | Forked; both directions. |
| `VersionProvider`, `DowngradeRegistrar`, `version/steps/`, `version/map/` stub transforms, `TransformContributor`, annotations (`@Stub`/`@Modify`/`@Adapter`/…), `Flags`, `cli/` | stays jvmdg | jvmdg-domain machinery — nothing to mirror. |

## Convergence notes

- unimined's `InheritanceBackedMappingTree`/`LazyASMInheritanceTree`/
  `DeltaMappingTree` solve the same problem family as
  `MemberResolver`/`NamespaceView` (position/propagation-aware member lookup
  over a hierarchy index). The commons-asm substrate should aim to host both
  models rather than fork a third mechanism.
- `JarStep`/`FusedJarRun` is a Java port of unimined-remap's Kotlin
  `FusibleJarStep`. Divergences: jvmdg's Java 8 baseline means abstract
  classes instead of `fun interface`/default methods in some ports; jvmdg
  steps emit whole-run artifacts
  (transformed class sets, shaded api classes, synthesized manifests) so the
  sink is reachable through `StepContext.sink` for the whole run — the
  unimined model only flushes metrics at `endFused`. Whether unimined-remap
  re-bases onto the commons-asm copy is an open question.

## Verification

Ports are checked with `./gradlew :commons-asm:test` (or the relevant module)
in the commons repo — JUnit 5 on JDK 17 via `autoToolchain`. jvmdg-side
changes are gated by `./gradlew :testing:test` (JDK 8→27 matrix).
