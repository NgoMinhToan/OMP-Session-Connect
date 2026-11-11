# OMP Session Connect

An Android client for [oh-my-pi (omp)](https://omp.sh) collab sessions. Scan the QR code printed by `/collab`, then read, steer, and monitor several OMP rooms from one phone.

> Third-party project. It is not affiliated with oh-my-pi. Collab links are secrets: a full link can prompt and interrupt the host agent.

## Features

- **QR pairing** — CameraX + ML Kit reads the QR rendered by `omp /collab`; pasting a link is the fallback.
- **Multiple rooms** — Every stored room keeps its own sealed WebSocket and transcript. Switching rooms is a list tap.
- **Live transcripts** — Streamed assistant text, thinking, tool cards with arguments/output, compaction and retry notices.
- **Run control** — Prompt, stop a turn, and answer host `ask` select/editor requests.
- **Voice dictation** — Android `SpeechRecognizer` with partial results, a live level meter, and a permission flow.
- **Session commands** — A draft starting with `/` is sent to the host Main agent through the collab `agent-cmd` channel, so the session's extension/custom/file commands and prompt templates run on the host. Built-in TUI commands dispatched before the session prompt remain host-only.
- **Subagent viewer** — Lists host subagents with status and fetches their JSONL transcript on demand (with refresh).
- **Todo progress** — The latest `todo` tool result is parsed into a compact strip; tap it to see every task, phase, status, and blocker, not only the active item. It also drives live notification updates.
- **Theme** — System / light / dark, persisted locally.
- **Status at a glance** — Connecting, live, working, waiting for input, read-only, reconnecting, ended, error.
- **Notifications** — Approval requests, finished turns, agent errors/lost connections, plus a silent live notification per running session showing the newest message and (optionally) todo progress. Each category is toggled in Settings.
- **Close old rooms** — Close one room from its card/menu, or bulk-close ended/failed rooms from the overflow menu.
- **Read-only support** — A 32-byte view link can watch a room but never prompt or answer.

## Requirements

- Android 8.0 (API 26) or newer.
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

## Architecture

```
app/      Compose UI, Navigation 3, Hilt entry points, foreground service, notifications
data/     collab link parser, AES-GCM wire transport, session state machine, DataStore, repository impl
domain/   pure Kotlin models, repository contract, use cases (no Android dependencies)
```

Dependency rule: `app → data, domain`; `data → domain`; `domain` depends only on Kotlin coroutines.

- **UI**: Material 3, dynamic color on Android 12+, edge-to-edge, `collectAsStateWithLifecycle`, immutable list rows with stable keys/content types. Settings apply the theme at the activity root.
- **State**: One `SessionConnection` owns each room on a single-threaded dispatcher; frames apply in arrival order. Streaming updates publish a new immutable snapshot but reuse the transcript list. Todo phases come from successful `todo` tool results.
- **Persistence**: DataStore holds a JSON index of sessions plus app settings; each collab link is sealed with an AES-256-GCM key in Android Keystore. Backups are disabled.
- **Background**: `SessionService` is a `specialUse` foreground service that keeps sockets alive and posts status notifications. It stops when the last room is closed.
- **Security**: Only `wss://` is accepted remotely (plain `ws://` is limited to localhost). Room keys never leave the link parser/cipher. No OkHttp logging interceptor is installed.

## Build

```sh
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device/emulator:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Release (R8 + resource shrinking, unsigned):
./gradlew :app:assembleRelease
```

Tests (24 JVM tests, including a MockWebServer handshake/turn test):

```sh
./gradlew :data:testDebugUnitTest
```

Release builds enable R8 and resource shrinking. The app is not configured for Play signing; add your own signing config before publishing.

## Installed agent skills

These skills were installed **inside the project** with `npx skills add … --copy` and are loaded from `.agents/skills/` (mirrored to `.claude/skills/`):

| Skill | Source |
| --- | --- |
| `mobile-android-design` | `wshobson/agents/plugins/ui-design/skills/mobile-android-design` |
| `android-clean-architecture` | `affaan-m/ecc` |
| `android-compose-state-effects` | `krutikjain/android-agent-skills` |
| `jetpack-compose-performance` | `android/skills` |
| `compose-animations` | `chrisbanes/skills` |

`skills-lock.json` pins the exact sources. Restore them with `npx skills experimental_install` (or `npx skills i`) in this directory.

## Protocol notes

See [docs/PROTOCOL.md](docs/PROTOCOL.md) for the link grammar, envelope layout, frame set, and reconnection behavior.

## Known limits

- The hosted relay (`my.omp.sh`) is the default; self-hosting the production relay is not distributed by omp.
- Android 15+ `specialUse` foreground services can be restricted by OEM battery policies. The service requests no exemption.
- The transcript renders text, thinking, tool cards, notices, and todo progress; full Markdown layout is intentionally out of scope.
- Voice dictation depends on the device's speech recognition service (some locales/offline modes may be unavailable).
- Collab exposes only prompt, abort, agent-cmd, and UI responses. Host-UI built-ins such as `/model`, `/compact`, and `/resume` are dispatched before `AgentSession.prompt` and cannot be invoked by a collab guest; use the host terminal for those.
- A room added twice is deduplicated by room key. Close a view-only room before adding its control link, because the first link wins.
