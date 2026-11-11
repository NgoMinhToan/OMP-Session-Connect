package com.ompsessionconnect.domain.model

/** Lifecycle of one remote collab room as observed by the phone. */
enum class SessionPhase {
  CONNECTING,
  LIVE,
  RECONNECTING,
  ENDED,
  FAILED,
}

/** Coarse status shown in lists, notifications, and the session header. */
enum class SessionStatus {
  CONNECTING,
  LIVE,
  WORKING,
  WAITING_INPUT,
  WATCHING,
  RECONNECTING,
  ENDED,
  ERROR,
}

data class SessionSummary(
  val id: String,
  val title: String,
  val roomId: String,
  val cwd: String?,
  val readOnly: Boolean,
  val createdAt: Long,
  val lastActiveAt: Long,
)

data class SessionOverview(
  val summary: SessionSummary,
  val snapshot: SessionSnapshot?,
) {
  val id: String get() = summary.id
  val title: String get() = snapshot?.title?.takeIf { it.isNotBlank() } ?: summary.title
  val status: SessionStatus get() = snapshot?.status ?: SessionStatus.CONNECTING
  val needsAttention: Boolean get() = snapshot?.needsAttention == true
}

enum class TranscriptItemType { USER, ASSISTANT, TOOL, NOTICE, SUMMARY }

sealed interface TranscriptItem {
  val id: String
  val timestamp: Long
  val itemType: TranscriptItemType
}

data class UserMessageItem(
  override val id: String,
  val text: String,
  val imageCount: Int,
  val from: String?,
  override val timestamp: Long,
) : TranscriptItem {
  override val itemType: TranscriptItemType = TranscriptItemType.USER
}

data class AssistantMessageItem(
  override val id: String,
  val text: String,
  val thinking: String,
  val model: String?,
  override val timestamp: Long,
) : TranscriptItem {
  override val itemType: TranscriptItemType = TranscriptItemType.ASSISTANT
}

enum class ToolStatus { RUNNING, DONE, ERROR }

data class ToolCallItem(
  override val id: String,
  val toolName: String,
  val arguments: String,
  val status: ToolStatus,
  val output: String?,
  override val timestamp: Long,
) : TranscriptItem {
  override val itemType: TranscriptItemType = TranscriptItemType.TOOL
}

data class NoticeItem(
  override val id: String,
  val text: String,
  val level: NoticeLevel,
  override val timestamp: Long,
) : TranscriptItem {
  override val itemType: TranscriptItemType = TranscriptItemType.NOTICE
}

data class SummaryItem(
  override val id: String,
  val text: String,
  override val timestamp: Long,
) : TranscriptItem {
  override val itemType: TranscriptItemType = TranscriptItemType.SUMMARY
}

enum class NoticeLevel { INFO, WARNING, ERROR }

data class SessionNotice(
  val id: Long,
  val text: String,
  val level: NoticeLevel,
  val timestamp: Long,
)

enum class UiRequestKind { SELECT, EDITOR }

data class UiOption(
  val label: String,
  val description: String? = null,
)

data class UiRequest(
  val requestId: Int,
  val kind: UiRequestKind,
  val title: String,
  val options: List<UiOption> = emptyList(),
  val prefill: String? = null,
  val helpText: String? = null,
  val selectionMarker: String? = null,
  val checkedIndices: Set<Int> = emptySet(),
)

enum class TodoStatus { PENDING, IN_PROGRESS, COMPLETED, ABANDONED, BLOCKED }

data class TodoTask(
  val phase: String,
  val content: String,
  val status: TodoStatus,
  val blocker: String? = null,
)

data class TodoProgress(
  val tasks: List<TodoTask>,
) {
  val total: Int get() = tasks.size
  val completed: Int get() = tasks.count { it.status == TodoStatus.COMPLETED }
  val active: TodoTask? get() = tasks.firstOrNull { it.status == TodoStatus.IN_PROGRESS }
  val next: TodoTask? get() = active ?: tasks.firstOrNull { it.status == TodoStatus.PENDING }
  val isComplete: Boolean get() = tasks.isNotEmpty() && tasks.all {
    it.status == TodoStatus.COMPLETED || it.status == TodoStatus.ABANDONED
  }
}

data class SessionSnapshot(
  val sessionId: String,
  val title: String,
  val roomId: String,
  val cwd: String?,
  val phase: SessionPhase,
  val readOnly: Boolean,
  val working: Boolean,
  val modelName: String?,
  val contextPercent: Int?,
  val contextTokens: Int?,
  val contextWindow: Int?,
  val queuedMessages: Int,
  val participantCount: Int,
  val activeAgents: Int,
  val loadingReceived: Int,
  val loadingTotal: Int,
  val transcript: List<TranscriptItem>,
  val streamingText: String?,
  val streamingThinking: String?,
  val uiRequest: UiRequest?,
  val notices: List<SessionNotice>,
  val todos: TodoProgress?,
  val endedReason: String?,
  val lastUpdated: Long,
) {
  val canPrompt: Boolean get() = phase == SessionPhase.LIVE && !readOnly
  val canAbort: Boolean get() = phase == SessionPhase.LIVE && !readOnly && working
  val needsAttention: Boolean get() = uiRequest != null || phase == SessionPhase.FAILED

  val status: SessionStatus
    get() = when (phase) {
      SessionPhase.CONNECTING -> SessionStatus.CONNECTING
      SessionPhase.RECONNECTING -> SessionStatus.RECONNECTING
      SessionPhase.ENDED -> SessionStatus.ENDED
      SessionPhase.FAILED -> SessionStatus.ERROR
      SessionPhase.LIVE -> when {
        uiRequest != null -> SessionStatus.WAITING_INPUT
        working -> SessionStatus.WORKING
        readOnly -> SessionStatus.WATCHING
        else -> SessionStatus.LIVE
      }
    }
}

sealed interface SessionAlert {
  val sessionId: String
  val title: String

  data class WaitingForInput(
    override val sessionId: String,
    override val title: String,
    val requestTitle: String,
  ) : SessionAlert

  data class TurnFinished(
    override val sessionId: String,
    override val title: String,
  ) : SessionAlert

  data class ConnectionLost(
    override val sessionId: String,
    override val title: String,
    val reason: String,
  ) : SessionAlert

  data class Ended(
    override val sessionId: String,
    override val title: String,
    val reason: String,
  ) : SessionAlert

  data class AgentError(
    override val sessionId: String,
    override val title: String,
    val message: String,
  ) : SessionAlert

  /** Ongoing, silent live notification for a running session. */
  data class Live(
    override val sessionId: String,
    override val title: String,
    val message: String?,
    val completed: Int,
    val total: Int,
    val current: String?,
  ) : SessionAlert
}
