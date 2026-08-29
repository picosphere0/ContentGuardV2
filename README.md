# ContentGuard

A personal accountability app. No DNS/VPN blocking - just two things:
1. Real friction against uninstalling it.
2. On-screen keyword-based blocking, disabled only after reading a fixed
   scripture passage and typing a fixed confession paragraph exactly.

## What this does

- **Device Admin**: uninstalling requires deactivating admin rights first,
  and deactivating admin rights requires going through the in-app gate.
- **Accessibility Service**: scans on-screen text across all apps for
  keywords you set, and backs out to the home screen the moment it finds
  a match.
- **Disable gate**: the only way to turn off Device Admin protection from
  inside the app. Shows James 1:14-15 (KJV), then requires typing a fixed
  confession paragraph exactly, word for word, before the deactivate
  button becomes enabled.
- **Watchdog**: a background notification that reminds you if Accessibility
  has been silently turned off by the system.

## What this cannot do

No third-party app can literally prevent Android from uninstalling it.
A factory reset, or ADB with USB debugging enabled, always wins - no
exceptions without full MDM enterprise enrollment on the device. This app
gets you real friction against impulsive removal, not literal impossibility.

Also worth knowing plainly: Accessibility Service can be turned off directly
from system Settings without ever touching the in-app gate - no app can
block access to its own Settings toggle. The gate here guards uninstalling
and gates the "I'm turning this off" moment inside the app; it can't stop
someone from going around it through Settings. If you want that closed too,
the next step up is Android Device Owner mode via ADB on a factory-reset
device (see note at the bottom).

## Setup (phone-only, via GitHub Actions)

1. In your GitHub repo, delete the old `contentguard-project.zip` and
   upload this new one in its place.
2. Make sure `.github/workflows/build.yml` in the repo matches the version
   below (edit it directly on github.com if it's already there):

```yaml
name: Build APK

on:
  push:
    branches: [ main ]
  workflow_dispatch: {}

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Unzip project
        run: |
          unzip -q contentguard-project.zip
          shopt -s dotglob
          mv contentguard/* .
          rmdir contentguard

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build debug APK
        run: gradle assembleDebug --no-daemon

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: contentguard-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

3. Commit to `main` - this triggers the build automatically.
4. Go to the **Actions** tab, wait for the green check, open the run,
   download the `contentguard-apk` artifact, unzip it, install the `.apk`.

## First-run setup inside the app

1. **Enable Device Admin** - accept the system prompt.
2. **Enable Accessibility Service** - opens system Settings, find
   "ContentGuard," turn it on.
3. Optionally edit the keyword list from the main screen (sensible
   defaults are already loaded).

That's it - no VPN prompt, no passphrase to set up front.

## Going further: Device Owner mode

If you want Accessibility itself to be unremovable from Settings (not
just uninstall), that requires Android's Device Owner mode, set via
`adb shell dpm set-device-owner` on a **factory-reset device with no
Google account added yet**. That unlocks real MDM-grade restrictions -
but it's a bigger, harder-to-reverse commitment, and needs a computer
with ADB for the one-time setup step (can't be done from the phone
alone). Say the word if you want to go there and I'll walk you through it.
