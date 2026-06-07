# 🌙 MoonPic

> An open-source Android image toolbox. Crop, rotate, resize, color-correct, batch-process, OCR, scan documents, apply LUTs, remove backgrounds, and much more — all on-device, privacy-first, no cloud required.

MoonPic is being built as an Apache-2.0 community alternative to commercial image editors, with a multi-module architecture and a focus on performance, modularity, and a clean Material 3 UI.

---

## ✨ Status: v0.1 in progress

This is the **v0.1 scaffold** release. What's working end-to-end right now:

- [x] Multi-module Gradle build (`app/` + `core/` + `feature/` + `lib/`)
- [x] Material 3 dynamic theme + dark mode
- [x] Home screen with feature grid
- [x] **Image picker** (Photo Picker / `ACTION_OPEN_DOCUMENT`)
- [x] **Crop** with aspect-ratio presets + freeform
- [x] **Rotate** (90° / 180° / 270° + arbitrary angle)
- [x] **Resize** (longest-edge / width × height / scale)
- [x] **Save** to MediaStore (Pictures/MoonPic) or app-private dir
- [x] **LUT import** — pick a `.cube` file, parse it, store in Room

What's coming in subsequent milestones:

- [ ] LUT real-time preview (OpenGL ES shader)
- [ ] Camera module (CameraX + live LUT overlay)
- [ ] Document scan (OpenCV edge detect + perspective correction)
- [ ] OCR (ML Kit + Tesseract fallback)
- [ ] PDF (image → PDF, searchable PDF, PDF → images)
- [ ] GIF maker / frame extractor
- [ ] Collage maker
- [ ] Batch processing
- [ ] AI background removal / super-resolution (ONNX Runtime)
- [ ] Watermark & signature
- [ ] EXIF editor
- [ ] 310+ filter library

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the full timeline.

---

## 🏗 Architecture

Multi-module, clean-architecture flavored:

```
moonpic/
├── app/                       Application module (MainActivity, navigation host)
├── core/
│   ├── core-ui/               Compose theme, design tokens, common components
│   ├── core-utils/            Extension functions, IO helpers, file utils
│   ├── core-data/             Repositories
│   ├── core-domain/           Use cases, domain models
│   ├── core-db/               Room database, DAOs, entities
│   ├── core-filters/          Filter/LUT engine (OpenGL ES)
│   ├── core-camera/           CameraX wrapper
│   ├── core-ocr/              OCR engine (ML Kit + Tesseract)
│   └── core-compress/         libjpeg-turbo wrapper
├── feature/
│   ├── feature-main/          Home screen
│   ├── feature-editor/        Single-image editor (crop/rotate/resize/adjust)
│   ├── feature-filters/       Filter library + LUT manager
│   ├── feature-camera/        Camera with LUT overlay
│   ├── feature-ocr/           OCR UI
│   ├── feature-scan/          Document scan UI
│   ├── feature-pdf/           PDF tools
│   ├── feature-gif/           GIF tools
│   ├── feature-collage/       Collage maker
│   ├── feature-batch/         Batch processing
│   └── feature-settings/      Settings
└── lib/
    ├── lib-opencv/            OpenCV for Android wrapper
    ├── lib-neural/            ONNX Runtime + AI model loader
    └── lib-native/            C++ acceleration (libjpeg-turbo, etc.)
```

**Tech stack:** Kotlin 2.0.21 · Jetpack Compose (BOM 2024.12.01) · Material 3 · Hilt · Coroutines + Flow · Coil · Room · DataStore · CameraX · OpenGL ES 3.0 · Navigation Compose · AGP 8.7.2 · Gradle 8.10.2.

Target: `minSdk 24` (Android 7.0) · `targetSdk 35` · `compileSdk 35`.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for deeper rationale and [`docs/ADR-001-multi-module.md`](docs/ADR-001-multi-module.md) for the modularization decision record.

---

## 🚀 Build

### Requirements
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17 (bundled with recent Android Studio)
- Android SDK Platform 35 + Build-Tools 35.0.0
- NDK 26+ (only for `lib-native` and `lib-opencv` once they're integrated)

### From the command line

```bash
# Debug APK
./gradlew :app:assembleDebug

# Install on a connected device
./gradlew :app:installDebug

# Unit tests
./gradlew test
```

### From GitHub Actions
Every push runs the `build-debug-apk.yml` workflow. After it completes, download the APK from **Actions → workflow run → Artifacts → `moonpic-debug-apk`**.

---

## 🤝 Contributing

We welcome PRs. Please:
1. Open an issue first for non-trivial changes.
2. Keep modules decoupled — features depend on `core/`, never on each other.
3. Run `./gradlew detekt ktlintCheck test` before pushing.
4. **Never commit secrets, keystores, API keys, or `.env` files.** All credentials belong in GitHub Actions Secrets or local environment variables.

---

## 📜 License

[Apache License 2.0](LICENSE).

ImageToolbox is a major inspiration for the architecture and filter library scope. We are not a fork; we are a clean-room re-implementation with our own design language.

---

## 🙏 Acknowledgments

- [ImageToolbox](https://github.com/T8RIN/ImageToolbox) — architecture & filter reference
- [Photon Camera](https://github.com/PhotonCam/PhotonCamera) — LUT engine reference
- [android-gpuimage](https://github.com/cats-oss/android-gpuimage) — OpenGL filter base
- The Compose, Hilt, CameraX, and Coil communities
