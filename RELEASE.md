# Jumper — Release Guide (Google Play)

This document describes how to build a signed release of Jumper and what to
prepare for a Google Play submission.

## 1. Prerequisites

- Android Studio (latest stable) or command-line Android SDK.
- JDK 17.
- The project uses the Gradle wrapper (`./gradlew`), Gradle 8.11.1 — no
  separate Gradle install is needed.

## 2. Create a release keystore (one time)

Run this once and keep the generated file safe and backed up. If you lose it
you cannot ship updates to the same app listing.

```bash
keytool -genkeypair -v \
  -keystore jumper-release.keystore \
  -alias jumper \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

## 3. Configure signing

Copy the template and fill in real values:

```bash
cp keystore.properties.template keystore.properties
```

Edit `keystore.properties`:

```
storeFile=/absolute/path/to/jumper-release.keystore
storePassword=********
keyAlias=jumper
keyPassword=********
```

`keystore.properties` and `*.keystore` are git-ignored — never commit them.
If `keystore.properties` is absent the release build still configures, but
the artifact is unsigned.

## 4. Build the release artifact

Android App Bundle (recommended for Google Play):

```bash
./gradlew bundleRelease
# output: app/build/outputs/bundle/release/app-release.aab
```

APK (for direct testing):

```bash
./gradlew assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

The release build enables R8 minification and resource shrinking
(`app/proguard-rules.pro` holds the keep rules for Room, Navigation and the
custom `JumperGameView`).

## 5. Versioning

Bump these in `app/build.gradle.kts` for every Play release:

- `versionCode` — integer, must strictly increase each upload.
- `versionName` — user-visible string, e.g. `1.0.1`.

## 6. Pre-submission checklist

- [ ] `applicationId` is final (`com.businessdoomguy.jumper`). It cannot be
      changed after the first publish.
- [ ] App icon reviewed on a device (adaptive icon in `mipmap-anydpi-v26`).
- [ ] `targetSdk` meets the current Google Play requirement.
- [ ] Tested on a physical device: tilt control and edge-press control.
- [ ] Audio: music plays in menu and game, Settings switches mute correctly.
- [ ] Level unlock works: only level 1 open on a fresh install, next level
      unlocks after a win.
- [ ] Room migration: install the previous version, then this one, and
      confirm coins / progress survive.
- [ ] Replace synthesized SFX with real audio files in `res/raw`
      (`sfx_jump`, `sfx_booster`, `sfx_monster`, `sfx_hazard`, `sfx_win`,
      `sfx_lose`) — optional but recommended before launch.
- [ ] Privacy policy URL prepared (required by Google Play).
- [ ] Store listing assets prepared: feature graphic, screenshots, short and
      full description.
- [ ] Content rating questionnaire completed in the Play Console.
- [ ] Data safety form completed — the app stores game progress locally only
      and collects no personal data.

## 7. Notes

- The game is offline and stores all data locally via Room. No network
  permission is requested.
- Launcher icons are vector-based. If an OEM launcher renders them poorly,
  export raster PNGs from Android Studio's Image Asset tool into the
  `mipmap-*dpi` folders.
