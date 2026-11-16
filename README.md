# OMP Session Connect

An Android client for [oh-my-pi (omp)](https://omp.sh) collab sessions. Scan the QR code printed by `/collab`, then read, steer, and monitor several OMP rooms from one phone.

[![Release](https://img.shields.io/github/v/release/NgoMinhToan/OMP-Session-Connect?label=release)](https://github.com/NgoMinhToan/OMP-Session-Connect/releases/latest)
[![Release APKs](https://github.com/NgoMinhToan/OMP-Session-Connect/actions/workflows/release.yml/badge.svg)](https://github.com/NgoMinhToan/OMP-Session-Connect/actions/workflows/release.yml)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](#requirements)
[![Protocol](https://img.shields.io/badge/collab%20proto-3-informational)](docs/PROTOCOL.md)

> Third-party project. It is not affiliated with oh-my-pi. Collab links are secrets: a full link can prompt and interrupt the host agent.

## Download

Every push to `main` publishes a signed APK per ABI plus a universal APK for both
build variants. Grab the newest one from
**[Releases](https://github.com/NgoMinhToan/OMP-Session-Connect/releases/latest)**
or let the app update itself (Settings → **Updates**).

| Device | File to download |
| --- | --- |
| Most phones since 2017 (arm64) | `OMP-Session-Connect-<version>-release-arm64-v8a.apk` |
| Older 32-bit phones | `OMP-Session-Connect-<version>-release-armeabi-v7a.apk` |
| Android emulator | `OMP-Session-Connect-<version>-release-x86_64.apk` |
| Not sure | `OMP-Session-Connect-<version>-release-universal.apk` |
| Debugging a crash / wanting readable stack traces | swap `release` for `release-nonminify` |

```sh
adb install -r OMP-Session-Connect-<version>-release-arm64-v8a.apk
```

Or copy the APK to the phone and open it (allow install from unknown sources).
All assets are signed with the same key, so newer builds install over older ones.
Check `SHA256SUMS.txt` in the release assets to verify a download.

## Features

- **QR pairing** — CameraX + ML Kit reads the QR rendered by `omp /collab`; pasting a link is the fallback.
- **Live Glass UI** — A wallpaper-derived gradient field, translucent glass sheets, Figtree/JetBrains Mono typography, a three-tab dock, and a bottom-right add FAB. Semantic session colors stay fixed when dynamic color is on.
- **Multiple rooms** — Every stored room keeps its own sealed WebSocket and transcript. Switching rooms is a list tap.
- **Live transcripts** — Streamed assistant text, thinking, tool cards with arguments/output, compaction and retry notices.
- **Run control** — Prompt, stop a turn, and answer host `ask` select/editor requests.
- **Voice dictation** — Android `SpeechRecognizer` with partial results, a live level meter, and a permission flow.
- **Session commands** — A draft starting with `/` is sent to the host Main agent through the collab `agent-cmd` channel, so the session's extension/custom/file commands and prompt templates run on the host. Built-in TUI commands dispatched before the session prompt remain host-only.
- **Subagent viewer** — Lists host subagents with status and fetches their JSONL transcript on demand (with refresh).
- **Todo progress** — The latest `todo` tool result is parsed into a compact strip; tap it to see every task, phase, status, and blocker, not only the active item. It also drives live notification updates.
- **Activity** — A live digest of session state plus an in-memory log of status changes, notices, and todo changes. It never invents history and resets when the process dies.
- **Theme** — System / light / dark and an optional wallpaper dynamic color (Android 12+), persisted locally.
- **Status at a glance** — Connecting, live, working, waiting for input, read-only, reconnecting, ended, error.
- **Notifications** — Approval requests, finished turns, agent errors/lost connections, plus a silent live notification per running session showing the newest message and (optionally) todo progress. Each category is toggled in Settings.
- **Self-updates** — On launch the app asks GitHub for the newest release and offers it in-app. The download runs in a foreground service (progress notification, continues when the app is closed), then opens the system installer automatically while you are in the app or from the "update ready" notification. Settings shows the installed version and a manual check.
- **Close old rooms** — Close one room from its card/menu, or bulk-close ended/failed rooms from the overflow menu.
- **Read-only support** — A 32-byte view link can watch a room but never prompt or answer.

## Requirements

- Android 8.0 (API 26) or newer, arm64 or 32-bit arm (x86/x86_64 builds are for emulators).
- omp 18.x on the host. The wire protocol implemented here is **proto 3**.
- Network access to the collab relay (`wss://my.omp.sh` by default) or to a self-hosted relay reachable over `wss://`/localhost `ws://`.
- Camera permission for QR scanning; microphone permission for dictation; notification permission (Android 13+) for alerts.

## Use

1. On the computer, run an interactive omp session and `/collab` (or `/collab view` for read-only).
2. On the phone, open OMP Session Connect → **Scan QR**.
3. Scan the browser QR code (it contains `https://my.omp.sh/#…`) or paste the `room.key` / `wss://…` link.
4. Tap the card to open the transcript, type a prompt, and use **Stop** to interrupt.
5. Swipe/back to the list to switch rooms. Open the card menu to reconnect or close a room.

The host must stay awake and online; the phone owns no session state locally beyond the encrypted link index.

## Build

Requires JDK 17 and the Android SDK (compileSdk 37, build-tools 37). `local.properties` must point at the SDK, or set `ANDROID_HOME`.

| Command | What it produces |
| --- | --- |
| `./gradlew :app:assembleDebug` | `app/build/outputs/apk/debug/app-debug.apk` |
| `./gradlew :app:assembleRelease` | R8 + resource shrinking, signed, split per ABI + universal |
| `./gradlew :app:assembleReleaseNonMinify` | Same code and signing, **no** R8 and no shrinking |
| `./gradlew test` | JVM unit tests (`:domain`, `:data`, `:app`) |

Both release variants honour three CLI flags so a build can be reproduced:

```sh
./gradlew :app:assembleRelease -Pomp.versionName=1.0.42 -Pomp.versionCode=42
```

`assembleRelease` and `assembleReleaseNonMinify` each emit five APKs:
`armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`, and `universal`.

Install on a connected device/emulator:

```sh
adb install -r app/build/outputs/apk/release/app-arm64-v8a-release.apk
```

### Signing

Release APKs are signed with `keystore/omp-release.jks` (credentials in
`keystore/keystore.properties`). The keystore is committed on purpose so CI can
publish installable builds with no repository secrets. That also means anyone
who clones this repo can sign an APK with this package name — only install
releases downloaded from this repository, and verify against
`SHA256SUMS.txt`. Certificate SHA-256:

```
3B:7A:A9:07:9A:5E:DA:C5:1C:C4:77:D7:8A:68:8B:A3:79:A1:70:60:33:D0:72:B2:6B:30:73:57:1D:D9:CB:BA
```

`OMP_KEYSTORE_PASSWORD`, `OMP_KEY_ALIAS`, and `OMP_KEY_PASSWORD` override the
stored credentials if you rotate the key. See [docs/RELEASING.md](docs/RELEASING.md).

## Releases and versioning

`.github/workflows/release.yml` runs on every push to `main`, on `v*` tags, and
on manual dispatch:

1. runs `./gradlew test`;
2. assembles `release` and `releaseNonMinify` for all ABIs;
3. derives `versionCode` from `git rev-list --count HEAD` (strictly increasing)
   and `versionName` as `1.0.<count>`, or from the pushed tag / dispatch input;
4. renames the APKs to `OMP-Session-Connect-<version>-<variant>-<abi>.apk`, adds
   `SHA256SUMS.txt`, and publishes a GitHub Release tagged `v<version>`.

Nothing needs to be tagged by hand: pushing to `main` creates
`v1.0.<count>` and publishes it immediately. To cut a semantic version, push a
`v1.2.0` tag or run the workflow manually with a version input.

## Architecture

```
app/      Compose UI, Navigation 3, Hilt entry points, foreground service, notifications
data/     collab link parser, AES-GCM wire transport, session state machine, DataStore, repository impl
domain/   pure Kotlin models, repository contract, use cases (no Android dependencies)
```

Dependency rule: `app → data, domain`; `data → domain`; `domain` depends only on Kotlin coroutines.

- **UI**: Live Glass on Material 3, optional dynamic color on Android 12+, edge-to-edge, `collectAsStateWithLifecycle`, immutable list rows with stable keys/content types. Status colors live outside the Material scheme so wallpaper changes never recolor live/needs-input/error. Settings apply the theme at the activity root.
- **State**: One `SessionConnection` owns each room on a single-threaded dispatcher; frames apply in arrival order. Streaming updates publish a new immutable snapshot but reuse the transcript list. Todo phases come from successful `todo` tool results.
- **Persistence**: DataStore holds a JSON index of sessions plus app settings; each collab link is sealed with an AES-256-GCM key in Android Keystore. Backups are disabled.
- **Background**: `SessionService` is a `specialUse` foreground service that keeps sockets alive and posts status notifications. It stops when the last room is closed.
- **Security**: Only `wss://` is accepted remotely (plain `ws://` is limited to localhost). Room keys never leave the link parser/cipher. No OkHttp logging interceptor is installed.

## Protocol notes

See [docs/PROTOCOL.md](docs/PROTOCOL.md) for the link grammar, envelope layout, frame set, and reconnection behavior.

## Known limits

- The hosted relay (`my.omp.sh`) is the default; self-hosting the production relay is not distributed by omp.
- Android 15+ `specialUse` foreground services can be restricted by OEM battery policies. The service requests no exemption.
- The transcript renders text, thinking, tool cards, notices, and todo progress; full Markdown layout is intentionally out of scope.
- Activity is an in-memory UI digest, not a persisted audit log; it starts empty after a cold start.
- Voice dictation depends on the device's speech recognition service (some locales/offline modes may be unavailable).
- Collab exposes only prompt, abort, agent-cmd, and UI responses. Host-UI built-ins such as `/model`, `/compact`, and `/resume` are dispatched before `AgentSession.prompt` and cannot be invoked by a collab guest; use the host terminal for those.
- A room added twice is deduplicated by room key. Close a view-only room before adding its control link, because the first link wins.

## Agent skills

Design and architecture skills used while building this app are vendored under
`.agents/skills/` and mirrored in `.claude/skills/`; `skills-lock.json` pins the
upstream sources. Restore them with `npx skills experimental_install`.
