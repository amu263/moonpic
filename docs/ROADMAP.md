# MoonPic Roadmap

> Living document. Status reflects what **ships** in the v0.1 scaffold; v0.2–v1.0 are targets, not promises.

## v0.1 — Scaffold + vertical slice (this milestone)

**Status:** in progress

- [x] Multi-module Gradle structure (`app/ core/ feature/ lib/`)
- [x] Material 3 dynamic theme + dark mode
- [x] Hilt + Coroutines + Navigation Compose wired up
- [x] Home screen feature grid
- [x] **Image picker** (Photo Picker + `ACTION_OPEN_DOCUMENT` fallback)
- [x] **Crop** (drag-to-resize handles, rule-of-thirds grid)
- [x] **Rotate** (90° / 270°)
- [x] **Resize** (longest-edge slider)
- [x] **Save** to `Pictures/MoonPic` via MediaStore
- [x] **LUT import** — `.cube` parser + Room storage
- [x] GitHub Actions: debug APK on every push

**Acceptance:** the Actions-built debug APK installs on a clean Android 7.0+ device, picks a photo, lets the user crop/rotate/resize, saves a new image to the gallery, and can import a `.cube` LUT into the local library.

## v0.2 — Editor depth + filters

- [ ] Adjustments (exposure, contrast, saturation, white balance)
- [ ] Filter library (≥30 built-in filters: brightness, contrast, sharpen, blur, hue rotate, sepia, grayscale, invert, …)
- [ ] LUT real-time preview via OpenGL ES shader
- [ ] Undo/redo stack
- [ ] EXIF viewer/editor

## v0.3 — Camera

- [ ] CameraX integration (preview, photo, video)
- [ ] Live LUT overlay
- [ ] Real-time adjustments
- [ ] Motion Photo capture (where supported)

## v0.4 — Document scan + OCR

- [ ] Edge detection + perspective correction (OpenCV)
- [ ] Adaptive threshold / black-and-white modes
- [ ] ML Kit text recognition (default)
- [ ] Tesseract offline fallback
- [ ] Searchable PDF export

## v0.5 — Format tools

- [ ] Format conversion: JPEG / PNG / WebP / HEIC / AVIF / JXL / GIF / BMP / TIFF / PDF
- [ ] libjpeg-turbo-backed compression
- [ ] GIF maker + frame extractor
- [ ] Collage maker (grid, freeform, mosaic)
- [ ] PDF: images → PDF, extract images, OCR-layer PDF

## v0.6 — AI

- [ ] Background removal (ONNX U²-Net / ISNet)
- [ ] Super-resolution
- [ ] Denoise / JPEG artifact removal
- [ ] Auto colorize
- [ ] On-device model manager (download on first use, NNAPI delegate)

## v0.7 — Batch

- [ ] Batch crop / rotate / resize
- [ ] Batch format conversion + compression
- [ ] Batch watermark / filter
- [ ] Batch EXIF edit / strip

## v0.8 — Watermark & signature

- [ ] Text watermark (font, color, opacity, position, rotation)
- [ ] Image watermark (PNG with alpha, scale, blend mode)
- [ ] Frame / signature templates

## v0.9 — Polish

- [ ] Tablet/foldable adaptive layouts
- [ ] Internationalization (中文简体/繁体, English first; Weblate after launch)
- [ ] Material You dynamic theme on Android 12+
- [ ] A11y pass (TalkBack, large-font, high-contrast)
- [ ] Telemetry opt-in (off by default; FOSS flavor never has it)

## v1.0 — Public launch

- [ ] Multi-device QA pass
- [ ] Release-signed AAB
- [ ] GitHub Releases, F-Droid submission, Play Store
- [ ] User docs, contributor docs
- [ ] Public roadmap freeze

---

## Milestones as dates

These dates are **targets**, not commitments. MoonPic is developed in the open; actual delivery depends on real-world testing, contributor availability, and dependency churn.

| Version | Target |
|---|---|
| v0.1 | 2026-Q3 (scaffold + editor + LUT import) |
| v0.2 | 2026-Q4 |
| v0.3 | 2027-Q1 (camera) |
| v0.4 | 2027-Q1 (OCR + scan) |
| v0.5 | 2027-Q2 (formats) |
| v0.6 | 2027-Q2 (AI) |
| v0.7 | 2027-Q3 (batch) |
| v0.8 | 2027-Q3 (watermark) |
| v0.9 | 2027-Q4 (polish) |
| v1.0 | 2027-Q4 / 2028-Q1 |
