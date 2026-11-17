# Agent contribution rules

Hard constraints for changes in this repo. The architecture map and cookbook
live in the root [AGENTS.md](../AGENTS.md); this file is the do/don't list.

## Language level

- `src/main` and `src/shared` compile for **Java 8** — Java 8 language and
  APIs (lambdas, method refs, streams, `computeIfAbsent`,
  `@Repeatable`/default/static interface methods) are allowed.
- `src/shared` is compiled into the **api jar** — no heavyweight
  dependencies, no ASM, nothing that can't run on a bare JRE. Changes there
  affect every consumer; keep them minimal. The jvmdg functional interfaces
  (`util/Function`, `Consumer`, `IO*`) still exist for output-side
  compatibility.
- Test code (`testing/`) runs on modern toolchains — Java 8+ APIs are fine.

## Bytecode rules

- **Never emit `COMPUTE_FRAMES`.** Transforms hand-author `FrameNode`s or
  leave existing frames untouched; writers use `COMPUTE_MAXS` only.
- **Never call `System.exit`** in agent/classloader code paths — throw
  `IllegalClassFormatException` (or propagate the exception) instead.
- Generated class/member names must be pure functions of (class, site) —
  `jvmdg$…`/`jvmdowngrader$…` names carry no global counters or traversal
  order.
- Runtime-reachable output must not reference `org/objectweb/asm/**` — ASM is
  transform-time machinery (plus optional runtime backends), never a runtime
  dependency.

## Determinism

- All jar/resource output goes through the sorted write + pinned-timestamp
  sink (`JarEntrySink`/`OutputUtils.pinTimestamps`). Don't add unordered
  `HashMap` iteration into emission paths.
- Anything mutated inside `visitPathsAsync`/`forEachAsync` must be a
  concurrent structure — a plain `HashSet`/`HashMap` loses entries
  nondeterministically (this was the `apiClasses` race that dropped shaded
  api classes).

## Registry / stub rules

- Stub registration happens through `StubScanner`/`StubRegistry` at provider
  `ensureInit` — the registry freezes at stage entry; don't register during
  transform application.
- `member` claims (in tests and `@Ref`) use the **registered** descriptor —
  adapter return types remap to real JDK types at registration.
- The stub-parity corpus is the diff oracle: if a change intentionally alters
  output, add a probe or a `stubparity-known-diffs.txt` entry with a rationale
  comment — never silently.
- Don't copy JDK source into stubs — LGPL constraints; reimplement behavior.

## Workflow

- Match the existing code style; keep one class (or one cohesive concept) per
  file.
- Run `./gradlew :testing:test` before reporting done — the matrix is the
  parity gate. `untestable.txt` stays header-only (ratchet).
- Don't commit or push unless asked; never commit secrets.
- Commons-mirror fixes port both directions — see
  [commons-mirror.md](commons-mirror.md) for which side is canonical.
