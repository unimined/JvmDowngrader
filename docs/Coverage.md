# Coverage

The current api stubs cover most of the commonly used and easy `java.base` functions. 
See [Bytecode Manipulation](./Bytecode%20Manipulation.md) for more details on how.

Some stubs should be fairly easy to add while others are more difficult.

Some functions that access internals may not work on non OpenJDK java implementations.

There are multiple API areas that aren't currently present for various reasons, 
mostly due to their difficulty to reproduce; due to either size or complexity.
reproducing an api requires implementing it myself due to the difference in license; 
You can't just copy everything from the existing implementation.

for example:

* [FFM/FFI](https://docs.oracle.com/en/java/javase/21/core/foreign-function-and-memory-api.html)
  * it's big
  * it uses natives
  * would probably need to be on top of [JNA](https://github.com/java-native-access/jna) to implement easiest
* [ClassFile](https://docs.oracle.com/en/java/javase/22/vm/class-file-api.html)
  * it's big
* [Unix Sockets](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/UnixDomainSocketAddress.html)
  * hard to add to the socket creation function
  * hard to actually implement without natives

## Coverage report

`./gradlew :java-api:coverageReport` diffs adjacent `ct.sym` surfaces per
boundary and writes `java-api/build/coverage/<version>/`:

* `coverage-matrix.txt` — every classified member, one
  `module;owner;name;desc;kind=stub|parent|gained|missing|knownMissing`
  per line. `gained` members have no registered stub but are callable
  through the gained-interface machinery at runtime.
* `available.txt` / `gained.txt` / `parentOnly.txt` / `missing.txt` /
  `knownMissing.txt` — the same rows grouped by kind.
* `unmatched.txt` — registered impls that correspond to no surface member
  (ratchet; should stay near header-only).

`src/coverage/resources/known-missing.txt` is the honest-gap allowlist:
members listed there are deliberately-unimplemented and report as
`kind=knownMissing` instead of `missing`. All files except
`unmatched.txt`/`parentOnly.txt` ship in the api jar under
`META-INF/coverage`.

## Behavioral parity notes

Downgrading preserves the compiled program's semantics, but some runtime
behaviors are honest approximations or divergences:

* **Virtual threads are emulated, not scheduled.** `Thread.ofVirtual()` /
  `ofPlatform()` / `startVirtualThread()` / `isVirtual()` /
  `Thread.Builder` produce a `FakeVirtualThread` — a real daemon platform
  thread. There is no carrier scheduling, no pinning semantics, and the
  threads are visible to `ThreadMXBean` (real virtual threads are
  invisible). `Executors.newVirtualThreadPerTaskExecutor()` /
  `newThreadPerTaskExecutor()` run a `ThreadPerTaskExecutor` (one real
  thread per task). Programs that rely on virtual-thread scalability —
  tens of thousands of parked VTs — will exhaust OS threads.
* **`MissingStubError` ≠ `NoSuchMethodError`.** Unstubbed members that
  hit a stub body, a dispatch miss, or a `--noIndy` lowered call site
  throw `jvmdg`'s `MissingStubError` (an `Error`), while members that
  were never registered keep their native call sites and fail with the
  real `NoSuchMethodError`. Feature-probing idioms that catch
  `NoSuchMethodError`/`LinkageError` narrowly may miss `MissingStubError`
  unless they catch `Error`/`Throwable` — catch broadly, or prefer the
  coverage report to check a member before relying on it.
* **Wrapper identity.** Gained-interface and adapter sites produce
  `$Wrap` objects that delegate `equals`/`hashCode`/`toString` to the
  wrapped member (two wraps of one member compare equal). `==`,
  `synchronized`, `IdentityHashMap`, and the member's own
  `.equals(wrap)` still see `Object` identity — one object is sometimes
  a `$Wrap`, sometimes the raw member, depending on which flow path
  produced it. Do not key identity on gained/adapter values.
* **`Sealed:` jars.** Signature block files (`.SF`/`.RSA`/`.DSA`/`.EC`)
  and per-name manifest digests are stripped, and `Sealed` main/entry
  attributes are dropped when signatures are removed — a sealed,
  signature-stripped jar loses its seal rather than failing package
  sealing at runtime. An unsigned sealed jar keeps its seal.
* **Bootstrap class-path entries load untransformed.** Classes appended
  to the boot classpath (e.g. the api jar itself via `-Xbootclasspath/a`)
  load before transformers attach and will fail with unexplained
  `NoClassDefFoundError`/`NoSuchMethodError`. Keep stub/api classes on
  the regular class path or agent-instrumented path.
* **`RuntimeMXBean.getPid` is fragile.** The stub parses the bean name
  up to `@`; VMs with exotic `RuntimeMXBean` name formats can throw
  `NumberFormatException`. Prefer `ProcessHandle.current().pid()` (real
  on j9+) where available.
* **`java.version` / `Runtime.version()` report the real runtime** by
  design — feature-detection code sees the true target-JVM capability,
  not the authored source version.

