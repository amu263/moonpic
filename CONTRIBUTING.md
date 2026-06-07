# Contributing to MoonPic

Thanks for your interest! MoonPic is an Apache-2.0 project and we welcome PRs, issues, and discussion.

## Ground rules

1. **Open an issue first** for non-trivial changes (anything beyond a typo, small refactor, or obvious bug fix). This lets us discuss the design before code is written.
2. **One PR = one concern.** Don't mix a refactor with a feature, or a feature with a build-system change.
3. **Stay inside the dependency direction.** `app → feature → core → lib`. Features never depend on each other. If you need to share code between features, it belongs in `core/`.
4. **No secrets, ever.** Don't commit keystores, API keys, or `local.properties` with a real SDK path. The CI uses repository-level secrets.
5. **Keep Compose state hoisted.** Composables are pure renderers; state lives in a `ViewModel` exposed as `StateFlow<UiState>`.
6. **Tests or it didn't happen.** Every new feature module needs at least one unit test. The editor and filters modules need both unit and instrumentation tests before merging.
7. **Update docs.** If you change architecture, behavior, or a public API, update `docs/ARCHITECTURE.md` or `README.md` in the same PR.

## Local development

```bash
git clone https://github.com/amu263/moonpic
cd moonpic
cp local.properties.example local.properties
# edit local.properties to point at your Android SDK
./gradlew :app:assembleDebug
```

Open in **Android Studio Ladybug (2024.2.1)** or newer. JDK 17 is required (bundled with the IDE).

## Coding style

- Kotlin official style. Use **ktlint** (`./gradlew ktlintCheck`).
- Imports are sorted automatically — don't hand-sort.
- Prefer `val` over `var`; prefer immutable data classes.
- 4-space indent, no tabs.
- Comments explain *why*, not *what*.

## Module map

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) and [`docs/ADR-001-multi-module.md`](docs/ADR-001-multi-module.md).

## Commit messages

`area: short summary` (max 72 chars), followed by a body if needed.

Examples:
- `editor: handle EXIF rotation when loading from MediaStore`
- `filters: add .cube parser with domain-min/max support`
- `ci: cache Gradle modules between runs`

## Code of conduct

Be kind. Critique the code, not the person. See [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) (TODO: add this file before public launch).
