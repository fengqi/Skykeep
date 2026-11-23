<p align="center">
  <img src="packaging/icons/skykeep.svg" alt="Skykeep Notes logo" width="96" height="96">
</p>

<h1 align="center">Skykeep Notes</h1>

<p align="center"><strong>Apple Notes, on Android.</strong><br>A single-app build of Skykeep: just Notes, with an interface that closely follows the iOS Notes app. Same engine, same iCloud sync, nothing else.</p>

<p align="center">
  English · <a href="README.zh-CN.md">简体中文</a>
</p>

<p align="center">
  <a href="https://github.com/YashwanthKamireddi/Skykeep"><img src="https://img.shields.io/badge/based_on-Skykeep-2f6fd6" alt="Based on Skykeep"></a>
  <a href="https://github.com/fengqi/Skykeep/blob/main/README.md"><img src="https://img.shields.io/badge/main_branch-README-2f6fd6" alt="Main branch README"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-2f6fd6" alt="GPL-3.0 license"></a>
</p>

## What this branch is

This is the `notes-ios` branch of a [Skykeep](https://github.com/YashwanthKamireddi/Skykeep) fork. Skykeep is an open-source (GPL-3.0) project that brings iCloud to Windows, Linux and Android with a Rust engine at its core; its main branch ships six apps — Notes, Reminders, Calendar, Contacts, iCloud Drive and Photos. See the [main branch README](https://github.com/fengqi/Skykeep/blob/main/README.md) for the full project.

This branch does one thing: **turn the Android app into Notes only**, with an interface that recreates the iOS Notes app as closely as possible. The upstream home screen and the other five apps are still in the code but no longer reachable — the app launches straight into Notes.

- **Same engine.** The Rust crates (~68k lines) are untouched. All changes live in the Android UI layer, so merging upstream updates stays cheap.
- **Installs alongside the original.** Package name `io.github.yashwanthkamireddi.skykeep.notes`, app name "Notes", its own yellow icon.
- **Same sync.** Signs in with your Apple Account; on first run it creates a Notes-only account on the device and disables the Contacts and Calendar sync adapters, so no permission dialogs for data this app doesn't use.

## Current status

Work in progress, as of November 2026. The core experience is in place:

- Folders screen: iCloud-pinned and system folders, your own folders, Recently Deleted at the bottom
- Search as iOS runs it — a mode, not a page
- iOS-style push/pop transitions, collapsing large titles, sticky search bar
- Note list: pinned notes, date grouping (Today / Yesterday / Previous 7 Days / …), FAB to compose, gallery view
- Select mode: multi-select, select all, move and delete
- Folder editing: drag to reorder, swipe for Move and Delete, one swipe open at a time
- Editor: read/write with debounced autosave, pin, move, delete; Trash with restore and delete-now

Not done yet:

- Viewing attachments by category (photos, websites, documents)
- A Notes settings screen
- iOS-style edge-swipe-back gesture
- Rich text editing, subfolders, unlocking locked notes

## Build

Requires the Android SDK, Rust, and cargo-ndk (the Gradle build compiles the engine automatically):

```sh
cd android
./gradlew :app:assembleDebug
```

The APK lands in `android/app/build/outputs/apk/debug/`.

## Acknowledgments and license

This fork exists thanks to [Skykeep](https://github.com/YashwanthKamireddi/Skykeep) by Yashwanth Kamireddi — this branch only reshapes its Android UI. Licensed under [GPL-3.0](LICENSE), same as upstream. Not affiliated with Apple Inc. Apple, iCloud and iPhone are trademarks of Apple Inc.
