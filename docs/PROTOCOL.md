# OMP collab protocol (proto 3)

This client implements the guest side of omp's collab relay. Reference sources at omp `v18.18.8`:

- `packages/coding-agent/src/collab/crypto.ts`
- `packages/coding-agent/src/collab/protocol.ts`
- `packages/coding-agent/src/collab/relay-client.ts`
- `packages/wire/src/index.ts`
- `packages/collab-web/src/lib/{link,socket,client,codec}.ts`

## Link grammar

A link carries the relay URL, a room id, and a secret. The secret is `base64url(key ∥ writeToken)`:

- 32 bytes → view-only key;
- 48 bytes → 32-byte AES key + 16-byte write token (full control).

Accepted forms:

```
<roomId>.<secret>                                  → wss://my.omp.sh/r/<roomId>.<secret>
<roomId>#<secret>                                  → legacy bare form
host[:port]/r/<roomId>.<secret>                    → wss:// inferred
wss://host[:port]/r/<roomId>.<secret>
ws://localhost:7475/r/<roomId>.<secret>            → localhost only
https://my.omp.sh/#<link>                          → browser deep link
https://web.example/collab/#relay.example/r/<room>.<secret>
```

Parsing is recursive on the fragment, so browser wrappers work. Remote plain `ws://` is rejected.

## Transport

- WebSocket: `GET wss://<relay>/r/<roomId>?role=guest`.
- Binary message: `[4-byte big-endian peerId][sealed payload]`.
  - Guest → relay: peerId `0`; the relay rewrites it to the guest id.
  - Host → relay: `0` broadcast or `N` targeted.
- Sealed payload: `[12-byte random IV][AES-256-GCM ciphertext+tag]`, plaintext is compact UTF-8 JSON.
- Text messages are unencrypted relay control frames; this client only reacts to `{"t":"room-closed"}`.

## Frames

Guest → host:

```json
{"t":"hello","proto":3,"name":"Android Pixel","writeToken":"<base64url>"}
{"t":"prompt","text":"..."}
{"t":"abort"}
{"t":"ui-response","reqId":7,"value":"yes"}
{"t":"agent-cmd","cmd":"chat","agentId":"…","text":"…"}
{"t":"fetch-transcript","reqId":1,"agentId":"…","fromByte":0}
```

Host → guest:

| `t` | Payload |
| --- | --- |
| `welcome` | `proto`, `header`, `state`, `agents`, `entryCount`, `readOnly` |
| `snapshot-chunk` | `entries[]`, `final` |
| `entry` | one session entry (message, custom_message, compaction, …) |
| `event` | agent events (`message_update`, `tool_execution_*`, notices, retries) |
| `state` | streaming flag, queue, model, context usage, participants |
| `agents` | agent registry snapshot |
| `ui-request` / `ui-request-end` | host `ask` select/editor request lifecycle |
| `bye` | terminal reason |
| `error` | message; `resync: true` means send `hello` again |

## Join and sync

1. Open the socket; send `hello` with proto 3 and the write token (absent for view links).
2. Wait ≤30 s for `welcome`. The host sends metadata first.
3. Accumulate `snapshot-chunk` frames until `final: true` or `entryCount` entries arrived.
4. Apply `entry` / `event` / `state` frames in order. Re-sync after any reconnect with a fresh `hello`.

The implementation mirrors the official web client's timeouts (30 s welcome and snapshot progress) and its reconnect policy (exponential backoff, fatal close codes 4009/4029).

## Notes for maintainers

- `SessionConnection` is the only writer of a room snapshot; keep it on one dispatcher.
- `TranscriptBuilder` folds `toolResult` entries into the tool card created by the assistant message, keyed by `toolCallId`.
- `TodoParser` reads `details.phases` from successful `todo` tool results (also via `tool_execution_end`), giving the UI and notification service a canonical task list.
- Streaming events carry the full accumulating message, not deltas.
- `readOnly` comes from the host's `welcome`, not only from the link length; respect the host.
- Do not add logging that prints raw frames or keys.
