# UI Context — OMP Session Connect

Ảnh chụp giao diện hiện tại của app Android, kèm danh sách màn hình, thành phần Compose chính và bảng màu.

- Thiết bị chụp: Samsung SM-A346E, Android 16 (API 36), 1080 × 2340.
- Ảnh nằm trong `design-context/screenshoots/` (đúng tên thư mục được yêu cầu).
- Dữ liệu trong ảnh session là các phòng demo tổng hợp; một card session thật xuất hiện trong ảnh danh sách.
- App dùng **dynamic color** trên Android 12+, nên màu trong ảnh lấy từ palette hình nền thiết bị (xanh lam). Bảng hex bên dưới là palette fallback của app khi dynamic color không khả dụng.

## 1. Danh sách màn hình

| # | Màn hình / trạng thái | Ảnh | Mục đích |
| --- | --- | --- | --- |
| 00 | Sessions — empty state | `00-sessions-empty.png` | Giới thiệu cách kết nối khi chưa có phiên |
| 01 | Sessions — danh sách nhiều phiên | `01-sessions-list.png` | Theo dõi nhiều phiên, trạng thái, todo/approval, mở phiên |
| 02 | Session detail — transcript | `02-session-detail.png` | Đọc transcript, tool card, todo, gửi prompt/voice |
| 03 | Session detail — host ask | `03-session-ask.png` | Trả lời yêu cầu select/editor của host |
| 04 | Session detail — session command | `04-session-command.png` | Gửi `/...` vào phiên qua Main agent |
| 05 | Add link dialog | `05-add-link.png` | Dán link collab hoặc room id |
| 06 | QR scanner | `06-qr-scanner.png` | Quét QR `/collab`, torch, fallback dán link |
| 07 | Settings — dark | `07-settings-dark.png` | Theme và bật/tắt thông báo |
| 08 | Settings — light | `08-settings-light.png` | Cùng màn hình ở theme sáng |
| 10 | Subagent list sheet | `10-subagents.png` | Danh sách subagent, trạng thái, mở transcript |
| 11 | Subagent transcript | `11-subagent-transcript.png` | Transcript riêng của subagent được fetch từ host |
| 12 | All todo tasks | `12-todo-all.png` | Xem toàn bộ task, không chỉ task đang active |

Ngoài app còn có **system notification**: notification foreground service, notification trạng thái (finished/error/approval), và notification live ongoing cho phiên đang chạy.

## 2. Thành phần Compose chính

### Sessions (empty + list)

- `Scaffold` + `TopAppBar`: tiêu đề Sessions, summary `N live · M need you`, action Settings / Add / More.
- `ExtendedFloatingActionButton`: Scan QR.
- `LazyColumn` với `items(key, contentType)`:
  - `SessionsSummaryCard`: `Surface` primaryContainer/errorContainer, số live và stored.
  - `SessionCard`: `Card` surfaceContainerHigh; `StatusPill` + `StatusDot`; read-only lock; relative last-active; model/context/queued text; `LinearProgressIndicator` cho syncing/working; excerpt approval; `DropdownMenu` Reconnect/Close.
- `EmptySessions`: icon, title/body, Scan QR / Paste link buttons.
- `AddLinkDialog`, `ConfirmDialog`, `SnackbarHost`.

### Session detail

- `Scaffold` + `TopAppBar`: title, `StatusPill`, read-only icon, Reconnect/Close actions.
- `StateBanners`: connecting/syncing, reconnecting, failed, ended; `LinearProgressIndicator`.
- `SessionMeta`: chip model, context %, participants, subagents.
- `LazyColumn(reverseLayout = true)`:
  - `UserBubble` — primaryContainer, guest attribution.
  - `AssistantBubble` — surfaceContainerHigh, thinking expand/collapse, streaming indicator.
  - `ToolCard` — surfaceContainer, args, status, expandable output, `animateContentSize`.
  - `NoticeRow`, `SummaryRow`.
- `SubagentBar` + `SubagentsSheet`: số subagent đang chạy/tổng, danh sách tên + trạng thái, mở transcript.
- `AgentTranscriptDialog`: fetch JSONL qua `fetch-transcript`, render bằng `TranscriptRow`, có Refresh.
- `TodoStrip`: tiến độ `completed/total`, progress bar, task hiện tại; bấm để mở `TodoSheet`.
- `TodoSheet`: toàn bộ task theo phase, status Pending/In progress/Completed/Abandoned/Blocked, blocker note.
- `AskPanel`: select options hoặc editor; trả lời host `ui-request`.
- `SessionCommandHint`: hiện khi draft bắt đầu bằng `/`.
- `Composer`: `OutlinedTextField` (trailing mic), Stop, Send; `LinearProgressIndicator` khi đang nghe.
- `ConfirmDialog` khi đóng phiên.

### QR scanner

- `Scaffold` + `TopAppBar`: back, title, Torch.
- `AndroidView(PreviewView)` + CameraX `ImageAnalysis`.
- `Canvas` overlay: scrim + khung quét bo góc + accent.
- `Surface` hint, `TextButton` Paste link, `AddLinkDialog`.
- Trạng thái quyền camera và loading khi thêm phiên.

### Settings

- `Scaffold` + `TopAppBar`.
- `LazyColumn`:
  - `SettingsHeader`.
  - `ThemeRow`: `RadioButton`, `selectable`, System/Light/Dark.
  - `ToggleRow`: `Switch` + `toggleable`; master, finished, errors, live todo, live session messages.
  - `NotificationPermissionRow`: trạng thái quyền, Allow notifications / Open settings.
  - `HorizontalDivider`.

### Notifications (system UI)

- `NotificationCompat` + 3 channel:
  - `session_status` — approval/finished/error (âm thanh mặc định).
  - `active_connections` — foreground service (low, silent).
  - `session_live` — live message/todo, `IMPORTANCE_LOW`, `sound=null`, silent, ongoing, `setOnlyAlertOnce(true)`.

## 3. Bảng màu

### Light — fallback scheme

| Token | Hex |
| --- | --- |
| primary | `#006A60` |
| onPrimary | `#FFFFFF` |
| primaryContainer | `#74F8E5` |
| onPrimaryContainer | `#00201C` |
| secondary | `#4A635F` |
| onSecondary | `#FFFFFF` |
| secondaryContainer | `#CCE7E2` |
| onSecondaryContainer | `#05201D` |
| tertiary | `#3F6374` |
| onTertiary | `#FFFFFF` |
| tertiaryContainer | `#C2E8FB` |
| onTertiaryContainer | `#001F2A` |
| background / surface | `#F4FBF8` |
| onBackground / onSurface | `#161D1B` |
| surfaceVariant | `#DAE5E1` |
| onSurfaceVariant | `#3F4946` |
| outline | `#6F7977` |

### Dark — fallback scheme

| Token | Hex |
| --- | --- |
| primary | `#53DBC9` |
| onPrimary | `#003731` |
| primaryContainer | `#005048` |
| onPrimaryContainer | `#74F8E5` |
| secondary | `#B1CCC7` |
| onSecondary | `#1C3531` |
| secondaryContainer | `#334B47` |
| onSecondaryContainer | `#CCE7E2` |
| tertiary | `#A6CCDF` |
| onTertiary | `#093544` |
| tertiaryContainer | `#264B5C` |
| onTertiaryContainer | `#C2E8FB` |
| background / surface | `#0E1513` |
| onBackground / onSurface | `#DEE4E1` |
| surfaceVariant | `#3F4946` |
| onSurfaceVariant | `#BEC9C6` |
| outline | `#889390` |

### Dynamic color

- Android 12+ (`Build.VERSION_CODES.S`): `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)`.
- Fallback palette ở trên chỉ dùng khi dynamic color không khả dụng hoặc bị tắt.
- Vì vậy ảnh chụp có thể dùng tông xanh lam theo hình nền, không trùng hex teal fallback.

### Màu ngữ nghĩa theo trạng thái

| Trạng thái | Token màu |
| --- | --- |
| Connecting / Reconnecting / Working | `tertiary` |
| Live | `primary` |
| Waiting input / Error | `error` |
| Watching (read-only) | `secondary` |
| Ended | `outline` |

- `StatusPill` dùng màu trạng thái với alpha 14% làm nền.
- Card cần chú ý có border `error` 1 dp.
- Summary card: `primaryContainer` khi ổn, `errorContainer` khi cần xử lý.
- User bubble: `primaryContainer`; assistant bubble / session card / todo strip: `surfaceContainerHigh`; tool card: `surfaceContainer`; ask panel: `tertiaryContainer`.

## 4. Typography & shape

- Material 3 `Typography()` mặc định.
- `titleLarge` / `titleMedium`: SemiBold; `titleSmall` / `labelLarge`: Medium.
- Tool args/output và command text: `CodeTextStyle` — monospace, 13 sp / 18 sp.
- Shape: Material 3 mặc định; card bo góc lớn (`shapes.large`/`extraLarge`), status pill dạng `CircleShape`.

## 5. Ghi chú ảnh chụp

- `06-qr-scanner.png`: camera preview màu đen vì lens thiết bị test bị che; overlay, toolbar, hint và fallback link là giao diện thật.
- `10-subagents.png` / `11-subagent-transcript.png` / `12-todo-all.png`: dùng phòng demo tổng hợp có 2 subagent và 4 todo task; không chứa dữ liệu phiên thật.
- `01-sessions-list.png`: có 3 phòng demo tổng hợp và 1 card phiên thật đang đồng bộ; không có nội dung hội thoại thật trong ảnh.
- `00-sessions-empty.png`: empty state của cùng build giao diện, chụp trước khi thêm dữ liệu demo.
