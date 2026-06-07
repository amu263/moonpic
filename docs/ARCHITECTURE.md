# MoonPic Architecture

> Status: **draft**, v0.1 scaffold. Will evolve as modules mature.

## Goals

1. **Privacy-first**: every operation on-device. No cloud, no telemetry by default.
2. **Modular**: features are decoupled; swapping a filter engine or storage layer doesn't ripple.
3. **Testable**: each module ships its own unit-test surface and depends only on `core-domain`/AndroidX.
4. **Performance**: GPU-accelerated image processing pipeline, on-device AI, low memory footprint.
5. **Open**: Apache-2.0, no Google-only dependencies in the FOSS flavor.

## Layered view

```
┌──────────────────────────────────────────────────────────┐
│ app/  ── single Activity, Navigation Compose host        │
├──────────────────────────────────────────────────────────┤
│ feature/  ── vertical slices (one feature = one module)   │
│   feature-main  feature-editor  feature-filters  …       │
├──────────────────────────────────────────────────────────┤
│ core/  ── horizontal capabilities shared by features     │
│   core-ui (theme, components)  core-data (repo)          │
│   core-domain (models, use-cases)  core-db (Room)        │
│   core-filters (LUT engine)  core-camera (CameraX)       │
│   core-ocr (Tesseract/ML Kit)  core-compress (jpeg-turbo)│
│   core-utils  (IO, bitmap, file helpers)                 │
├──────────────────────────────────────────────────────────┤
│ lib/  ── third-party/FFI wrappers                         │
│   lib-opencv  lib-neural (ONNX Runtime)  lib-native (C++)│
└──────────────────────────────────────────────────────────┘
```

**Direction of dependencies:** `app → feature → core → lib`. `feature` modules **never** depend on each other. Cross-feature communication goes through the `app` navigation host (route + arguments), through `core-data` repositories, or through `core-domain` events.

## Threading model

| Concern | Dispatcher |
|---|---|
| Compose UI | Main |
| Bitmap transforms (crop/rotate/resize) | `Dispatchers.Default` (CPU bound) |
| File I/O (decode, save, copy) | `Dispatchers.IO` |
| AI inference | `Dispatchers.Default` with NNAPI delegate |
| GPU work | OpenGL ES thread; bridged via `GLSurfaceView` or `Effect` |

We do **not** create new thread pools ad-hoc; coroutine `withContext` plus the shared `IODispatcher` / `DefaultDispatcher` is enough for v0.1.

## State management

- Compose UI is "dumb" — it renders state and emits intents.
- ViewModels (Hilt-injected) own the state, hold a `StateFlow<UiState>`.
- Repositories (`core-data`) are the only thing that talks to data sources (Room, filesystem, MediaStore).
- A small number of use-cases live in `core-domain` and orchestrate one repository call into a domain operation. We do **not** enforce clean-use-case-for-everything yet — v0.1 prefers a thin repository-only design to keep the surface area small.

## v0.1 implementation notes

- **`feature-editor`** is a single `EditorViewModel` that holds the source bitmap, the rotation accumulator, and the crop rect. The Compose layer renders the bitmap via `Image(bitmap = …)` and overlays `CropOverlay`. We deliberately use `Bitmap` rather than `ImageBitmap` throughout v0.1 for simplicity; we'll move to `ImageBitmap` / GPU-backed textures when the OpenGL pipeline lands.
- **`.cube` parsing** is a hand-rolled scanner in `core-filters/.../CubeParser.kt` that builds an in-memory `FloatArray(size³ × 3)`. Trilinear sampling is provided so v0.2 can run LUTs in software; the OpenGL ES shader can be a drop-in replacement later.
- **Storage** uses `MediaStore` on Android 10+ (no permission needed for write) and falls back to `WRITE_EXTERNAL_STORAGE` on lower API levels (which the manifest still declares for completeness).
- **DI** is Hilt. Database is exposed as a `@Singleton` in `core-db` and provided via a `@Module`. Features inject repositories, never the database directly.

## Open questions for later milestones

- Should `core-filters` ship with a built-in filter library (so the FOSS APK has 310+ filters out of the box) or stay minimal and let users import packs?
- AI model delivery: bundled, downloaded on first use, or Play Asset Delivery?
- Tablet/foldable layouts: do we ship a different feature grid at wide widths, or stick with adaptive grid for v1?
