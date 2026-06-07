# ADR-001: Multi-module Gradle layout

**Status:** accepted (v0.1)
**Date:** 2026-06-07
**Context:** the original MoonPic spec describes a feature-rich image toolbox (editor, camera, OCR, AI, batch, PDF, GIF, …) and lists several independent capability layers (filters, OCR, compression, …). Putting all of this in a single `:app` module was the obvious but wrong default.

## Decision

Adopt a **layered multi-module** structure:

```
app/  ──  core/ (ui, data, domain, db, filters, camera, ocr, compress, utils)
         lib/ (opencv, neural, native)
         feature/ (one Gradle module per user-facing feature)
```

Each `feature/*` module is independently buildable, testable, and (eventually) extractable. Modules use `api`/`implementation` dependencies to expose only what downstream consumers need.

## Rationale

1. **Build parallelism.** Gradle's task graph parallelizes module boundaries for free. A 5-minute monolith build shrinks to ~30s for a single-feature change.
2. **Feature isolation.** Adding a new `feature-collage` shouldn't require touching `feature-editor`; this protects both from regressions.
3. **Reproducible subsystems.** `core-filters`, `core-ocr`, and `core-compress` are all "engines" that could in principle be reused outside MoonPic. A clean module boundary makes that possible without a refactor.
4. **Test surface.** Unit tests live next to the code they exercise. We can run `:feature:feature-editor:test` without spinning up the whole app.

## Trade-offs

- **More boilerplate.** Each module needs its own `build.gradle.kts`, `AndroidManifest.xml`, and version-catalog mapping. We mitigate with templated generators and a strict `gradle/libs.versions.toml`.
- **Inter-module navigation needs a host.** The `app` module is the single `NavHost`. Features expose `fun fooScreen(onBack: …)` Composable entry points and a `NavGraphBuilder` extension; `app` wires them up.
- **DI scoping.** Hilt's `@HiltViewModel` and `@InstallIn(SingletonComponent::class)` work transparently across modules. We expose a small set of `@Module` providers in `core-db` and `core-data`; features never need their own Hilt modules in v0.1.

## Alternatives considered

- **Single-module.** Rejected: too much coupling, too slow to build, makes it impossible to swap engines (e.g. ML Kit → PaddleOCR) cleanly.
- **Hexagonal / clean architecture module split** (`adapter/`, `application/`, `domain/`, `infrastructure/`). Rejected: over-engineered for v0.1; we can refactor toward this if/when the codebase grows past a few hundred KLOC.
- **Per-feature self-contained bundles** (each feature owns its data + UI + DI). Rejected: would duplicate database, theme, and DI setup in 11 places.

## Consequences

- New contributors must follow the **dependency direction** strictly: `app → feature → core → lib`. A lint rule will eventually enforce this.
- The CI matrix is still single-job for v0.1; we'll split into per-module CI shards once the build grows.
- We document each module's purpose in its `build.gradle.kts` header comment.

## References

- Google's [Modularization Learning Pathway](https://developer.android.com/topic/modularization)
- Now in Android sample architecture
