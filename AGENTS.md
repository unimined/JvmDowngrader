# JvmDowngrader — Agent Notes

Gradle build (Kotlin DSL). Main source compiles for **Java 7**
(`mainVersion=7` in `gradle.properties`) — no Java 8+ APIs in
`src/main`/`src/shared` (no `computeIfAbsent`, no default interface methods,
no static methods on `@interface`). Test code targets whatever toolchain the
test JVM uses (Azul `testVersion`).

## Building & testing

- `./gradlew :testing:test` — unit tests + end-to-end downgrade matrix across
  every JDK from `testTargetVersion` to `testVersion` (Azul toolchains
  auto-provisioned via foojay-resolver).
- Tests run under JUnit 5 with **parallel `concurrent` execution** —
  tests that mutate JVM-global state (system properties, singletons) must be
  serialized (`@Execution(SAME_THREAD)`) or they will flake.
- CI: `.github/workflows/build.yml` and `build_snapshot.yml` run the full
  matrix in a `test` job before publishing.

## commons-asm / commons-core mirrors

`wagyourtail/commons` contains forked copies of several classes. Fixes must be
ported both directions — the forks have diverged (commons uses sync
`walkFileTree`, lombok, `commons.core` Logger/utils) so ports adapt rather than
copy:

| JvmDowngrader | commons counterpart |
|---|---|
| `src/main/.../jvmdg/compile/shade/ReferenceGraph.java` | `commons-asm/.../commons/asm/graph/ReferenceGraph.java` |
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
