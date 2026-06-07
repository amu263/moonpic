# Security Policy

## Reporting a vulnerability

Please **do not file a public GitHub issue** for security bugs.

Email **security@moonpic.dev** (or open a [GitHub Security Advisory](https://github.com/amu263/moonpic/security/advisories/new) once the repo is public). We aim to acknowledge within 3 business days and ship a fix or mitigation within 30 days for critical issues.

When reporting, please include:
- A description of the vulnerability and its impact
- Steps to reproduce (proof-of-concept code or screenshots)
- The affected version / commit hash
- Your name / handle for the credits section (optional)

## Threat model

MoonPic is a fully **on-device** Android image editor. The threats we care about are:

1. **Malicious image files** triggering memory corruption in the OpenCV / libjpeg-turbo / native decoders. We pin decoder versions, sandbox bitmap allocations, and recycle aggressively.
2. **Information disclosure** via logs, screenshots, or the share sheet. We do not log image content. The default `Pictures/MoonPic` output path is namespaced.
3. **Permission creep.** The v0.1 release only requests `INTERNET` (for the future remote LUT packs) and the camera/storage permissions required by the running feature. Future features must declare new permissions in their feature module's `AndroidManifest.xml`, never in `app/`.
4. **Supply chain.** All Gradle dependencies come from `google()` and `mavenCentral()` only (see `settings.gradle.kts`). We pin versions in `gradle/libs.versions.toml` and review upgrades in PRs.
5. **CI secret leaks.** All CI secrets live in **GitHub Actions Secrets**, never in repository files. The debug-APK workflow is secret-free; release signing will use `KEYSTORE_*` repository variables.

## What we will **never** ship

- Telemetry or analytics that leave the device without explicit opt-in
- Cloud uploads of user images (the FOSS flavor has no network usage at all)
- Hard-coded API keys, tokens, or service accounts
- Closed-source binaries whose source we cannot publish

## Acknowledgments

We credit reporters of valid vulnerabilities in the release notes (unless they prefer to stay anonymous).
