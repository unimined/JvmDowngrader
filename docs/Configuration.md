# Configuration

JvmDowngrader's flags are a single option surface — every option is declared
once on a `Flags` field via `@BoundFlag`, and that one declaration drives the
CLI flag, the system property, the `jvmdg.mf` config key, the Gradle DSL
property, `Flags.copy()`/`equals`/`toString`, and the `--print` dump. There is
nothing to keep in sync by hand; a field without `@BoundFlag` fails a static
completeness check at class-init.

## Value sources and precedence

Low → high; a higher source always wins:

1. **Built-in defaults** — the `Flags` field initializers.
2. **Embedded rules** — `META-INF/jvmdg/*.mf` inside input jars, sandboxed to
   the jar's own packages (see below).
3. **Config files** — `--config <file>` (repeatable, applied in order; later
   files win per key) or `jvmdg { config.from(file("jvmdg.mf")) }`.
4. **System properties** — a set `jvmdg.*` property mechanically skips config
   application for its flag.
5. **Explicit CLI flags / Gradle DSL values.**

## jvmdg.mf syntax

JAR-manifest syntax — the same shape `java.util.jar.Manifest` reads, parsed by
`ManifestConfigReader` (which additionally preserves section order, rejects
duplicates, accepts a missing final newline, imposes no line-length limit, and
reports `file:line` in errors):

```text
Jvmdg-Config-Version: 1
Target: 11
Log-Level: warn
Ignore-Warnings-In: com/example/generated/**

Name: com/example/legacy/**
Ignore-Warnings: true
```

- `Key: value` lines; keys are case-insensitive, `[A-Za-z0-9][A-Za-z0-9_-]*`.
- A blank line ends a section. `Name: <pattern>` opens a scoped section.
- A line starting with one space continues the previous value (manifest
  continuation — the leading space is stripped; a second space carries a
  separator into the value).
- `Jvmdg-Config-Version: 1` is required as the first key of the main section.
- Duplicate keys and duplicate `Name:` sections are errors.

## Keys

Every bound config key with its CLI flag, system property, and Gradle DSL
property. Value forms: `bool`, `int`, `enum` (`Log-Level`: `trace` `debug`
`info` `warn` `error` `fatal`), `java-version` (a Java major — `Target: 11`
→ class version 55; the matching CLI/sysprop takes the legacy class-version
opcode), `set` (comma/space/pipe-separated), `path-list` (platform path
separator), `wildcard-map` (comma-separated patterns).

| Key | CLI flag | System property | Gradle DSL | Form |
|---|---|---|---|---|
| `Target` | `--classVersion` / `-c` | `jvmdg.classVersion` | `downgradeTo: JavaVersion` | java-version / int |
| `Api` | `--api` / `-a` | `jvmdg.javaApi` | `apiJar` | path-list |
| `Quiet` *(deprecated)* | `--quiet` / `-q` | `jvmdg.quiet` | `quiet` | const-true |
| `Log-Ansi-Colors` | `--noColor` / `-nc` (const-false) | `jvmdg.logAnsiColors` | `logAnsiColors` | bool |
| `Log-Level` | `--logLevel` / `-l` | `jvmdg.logLevel` | `logLevel` | enum |
| `Ignore-Warnings-In` | `--ignoreWarningsIn` / `-i` | `jvmdg.ignoreWarnings` | `ignoreWarningsIn` | wildcard-map |
| `Allow-Maven` | — | `jvmdg.maven` | `allowMaven` | bool |
| `Print-Debug` *(deprecated)* | `debug --print` / `-p` | `jvmdg.debug` | `debug` | const-true |
| `Shade-Inlining` | `--disable-inlining` (const-false) | `jvmdg.shadeInlining` | `shadeInlining` | bool |
| `Exclude-Stub-Versions` | `debug --skipStubs` / `-s` | `jvmdg.debug.skipStubs` | `debugSkipStubs` | version-set |
| `Exclude-Jeps` | `debug --skipJEP` / `-sj` | `jvmdg.debug.skipJeps` | `debugSkipJeps` | int-set |
| `Debug-Dump-Classes` | `debug --dumpClasses` / `-d` | `jvmdg.debug.dumpClasses` | `debugDumpClasses` | const-true / bool |
| `Exclude-Stubs` | `debug --skipStub` / `-ss` | `jvmdg.debug.skipStub` | `debugSkipStub` | member-set |
| `Debug-No-Synthetic` | `debug --noSynthetic` / `-ns` | `jvmdg.debug.noSynthetic` | `debugNoSynthetic` | const-true / bool |
| `Debug-No-Fuse` | — | `jvmdg.debug.noFuse` | `debugNoFuse` | bool |
| `Debug-No-Stub-Idx` | — | `jvmdg.debug.noStubIdx` | `debugNoStubIdx` | bool |
| `Multi-Release-Original` | `--multiReleaseOriginal` / `-mro` | `jvmdg.multiReleaseOriginal` | `multiReleaseOriginal` | const-true / bool |
| `Multi-Release-Versions` | `--multiRelease` / `-mr` | `jvmdg.multiReleaseVersions` | `multiReleaseVersions` | version-set |
| `Multi-Release-Inputs` | `--multiReleaseInputs` / `-mri` | — | `downgradeFromMultiReleases` | const-true / bool |
| `Shade-Prefix` | — | — | `shadePrefix` | string |
| `Ignore-Embedded-Rules` | `--ignoreEmbeddedRules` | — | `ignoreEmbeddedRules` | const-true / bool |
| `Keep-Embedded-Rules` | `--keepEmbeddedRules` | — | `keepEmbeddedRules` | const-true / bool |

CLI-only (not config keys): `--config`, `--target`, `--classpath`, `--prefix`,
`--downgradedApi`, `bootstrap` args.

`--api`'s config/sysprop form uses the platform path separator; the Gradle
`apiJar` is a `ListProperty<File>`.

## Scoped sections

`Name: <pattern>` sections hold rules that apply to classes matching the
pattern, using the same wildcard semantics as `Ignore-Warnings-In`:

- `com/example/Thing` — exactly that class.
- `com/example/*` — direct members of the package (`com/example/Thing`,
  not `com/example/sub/Thing`).
- `com/example/**` — the package and everything under it.

Resolution is **order-independent: the most specific matching section wins**
(longer literal prefix, then narrower wildcard). Two different sections
matching the same class at equal specificity is a config error — file order
never decides semantics.

Scoped keys honored today:

- `Ignore-Warnings: true|false` — suppress/enable missing-member warnings
  for matching classes.

`Exclude-Jeps`/`Exclude-Stubs` are global-only until the per-class condition
store lands with `stubPolicy`; `Stub-Policy`, `On-Missing-Api`,
`Keep-Reflection`, and `Include-Stubs` arrive in the same phase. Unknown keys
and global keys in a `Name:` section are hard errors.

## Embedded rules

A jar may ship `META-INF/jvmdg/*.mf` files. During downgrade/shade,
`EmbeddedRulesStep` reads them (sorted order) **before any class is
transformed** and applies them *sandboxed*: every rule — including the file's
main section — is implicitly constrained to the set of package directories
the jar actually contains. A library's rules steer its own code and nothing
else. Named sections are intersected with the same package set. Global-only
and unknown keys are dropped with a warning (a newer jvmdg's key in a
dependency must not brick your build).

- Rules are **stripped from the output jar** by default.
- `Keep-Embedded-Rules: true` preserves them (re-export: the downgraded jar
  keeps steering its own consumers).
- `Ignore-Embedded-Rules: true` skips reading them *and* passes the files
  through untouched.

## Inspecting the result

- CLI: `jvmdg config --print` — every bound value plus its source
  (`default`/`config`/`sysprop`/`cli`), followed by the scoped rules.
- Gradle: `./gradlew jvmdgPrintConfig` — same dump for the merged DSL +
  config-file state.

## Adding a flag

Annotate the `Flags` field:

```java
@BoundFlag(key = "My-Key", cli = "--myFlag", sysprop = Constants.MY_PROP,
    gradle = "myProp", form = BoundFlag.Parser.BOOLEAN,
    doc = "What it does")
public boolean myFlag = false;
```

That is the whole change for CLI/sysprop/config/copy/equals/toString/`--print`.
Gradle needs the typed `Property` declaration on `DowngradeFlags` (the DSL is a
public API surface) plus one line in `GRADLE_APPLIERS` for the value adapter;
a binding naming a gradle property with no applier fails at runtime, and the
completeness check fails the build for any unbound field.
