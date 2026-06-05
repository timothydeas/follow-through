# Project rules

## Device & emulator testing — do NOT automate
- Never boot an emulator, install the app to a device, launch the app, drive the UI, or take device/emulator screenshots.
- Never run /verify (or anything equivalent) that does the above.
- I do all on-device testing myself on my physical Pixel.
- If you think device verification is genuinely needed, say so in plain text and STOP — do not start it. Wait for me to explicitly say go.
- Building, lint, and unit tests on this machine are fine and expected — just never touch a device or emulator.

## Versioning
- **Onboarding re-show:** Whenever onboarding content or copy changes, OR when Tim asks to re-see the onboarding flow, bump `CURRENT_ONBOARDING_VERSION` (in `AppNavigation.kt`) by 1 so onboarding shows again on the next launch. No user data is reset by this — it only advances the onboarding-seen gate.
- **App version:** After a batch of substantive changes intended for an on-device test build or a release, increment `versionCode` (in `app/build.gradle.kts`); also bump `versionName` for user-facing releases. Don't bump on trivial commits.
