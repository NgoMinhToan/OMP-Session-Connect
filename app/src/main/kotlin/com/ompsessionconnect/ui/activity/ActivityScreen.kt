package com.ompsessionconnect.ui.activity

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionStatus
import com.ompsessionconnect.ui.components.StatusDot
import com.ompsessionconnect.ui.components.statusColor
import com.ompsessionconnect.ui.components.statusLabel
import com.ompsessionconnect.ui.components.statusTextColor
import com.ompsessionconnect.ui.theme.LiveGlassShapes
import com.ompsessionconnect.ui.theme.LocalLiveGlass
import com.ompsessionconnect.ui.theme.LocalSessionStatusColors
import java.util.concurrent.atomic.AtomicLong
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf

enum class ActivityEventKind {
  ADDED,
  STATUS,
  NEEDS_INPUT,
  NOTICE,
  TODO,
  SUBAGENTS,
  REMOVED,
}

data class SessionActivityEvent(
  val id: Long,
  val sessionId: String,
  val sessionTitle: String,
  val kind: ActivityEventKind,
  val status: SessionStatus? = null,
  val text: String? = null,
  val count: Int? = null,
  val timestamp: Long = System.currentTimeMillis(),
)

private data class ActivityFingerprint(
  val title: String,
  val status: SessionStatus,
  val needsAttention: Boolean,
  val uiRequestTitle: String?,
  val todo: String?,
  val activeAgents: Int,
  val noticeId: Long?,
)

@Composable
fun rememberSessionActivityLog(sessions: List<SessionOverview>?): SnapshotStateList<SessionActivityEvent> {
  val events = remember { mutableStateListOf<SessionActivityEvent>() }
  var previous by remember { mutableStateOf<Map<String, ActivityFingerprint>>(emptyMap()) }
  var seeded by remember { mutableStateOf(false) }
  val nextId = remember { AtomicLong(1L) }

  LaunchedEffect(sessions) {
    val current = sessions ?: return@LaunchedEffect
    val next = current.associate { overview ->
      val notice = overview.snapshot?.notices?.lastOrNull()
      overview.id to ActivityFingerprint(
        title = overview.title,
        status = overview.status,
        needsAttention = overview.needsAttention,
        uiRequestTitle = overview.snapshot?.uiRequest?.title,
        todo = overview.snapshot?.todos?.next?.content,
        activeAgents = overview.snapshot?.activeAgents ?: 0,
        noticeId = notice?.id,
      )
    }
    if (!seeded) {
      previous = next
      seeded = true
      return@LaunchedEffect
    }

    fun add(
      overview: SessionOverview,
      kind: ActivityEventKind,
      status: SessionStatus? = null,
      text: String? = null,
      count: Int? = null,
    ) {
      events.add(
        0,
        SessionActivityEvent(
          id = nextId.getAndIncrement(),
          sessionId = overview.id,
          sessionTitle = overview.title,
          kind = kind,
          status = status,
          text = text,
          count = count,
        ),
      )
    }

    for (overview in current) {
      val old = previous[overview.id]
      if (old == null) {
        add(overview, ActivityEventKind.ADDED)
        continue
      }
      if (old.status != overview.status) {
        add(overview, ActivityEventKind.STATUS, status = overview.status)
      }
      if ((!old.needsAttention && overview.needsAttention) ||
        (old.uiRequestTitle == null && overview.snapshot?.uiRequest?.title != null)
      ) {
        add(overview, ActivityEventKind.NEEDS_INPUT)
      }
      val notice = overview.snapshot?.notices?.lastOrNull()
      if (notice != null && notice.id != old.noticeId) {
        add(overview, ActivityEventKind.NOTICE, text = notice.text)
      }
      val todo = overview.snapshot?.todos?.next?.content
      if (todo != null && todo != old.todo) {
        add(overview, ActivityEventKind.TODO, text = todo)
      }
      val agents = overview.snapshot?.activeAgents ?: 0
      if (agents != old.activeAgents && agents > 0) {
        add(overview, ActivityEventKind.SUBAGENTS, count = agents)
      }
    }
    previous.keys
      .filterNot { it in next.keys }
      .forEach { removedId ->
        events.add(
          0,
          SessionActivityEvent(
            id = nextId.getAndIncrement(),
            sessionId = removedId,
            sessionTitle = previous[removedId]?.title.orEmpty(),
            kind = ActivityEventKind.REMOVED,
          ),
        )
      }
    while (events.size > 40) events.removeAt(events.lastIndex)
    previous = next
  }
  return events
}

@Composable
fun ActivityScreen(
  sessions: List<SessionOverview>?,
  events: List<SessionActivityEvent>,
  onOpenSession: (String) -> Unit,
  listState: LazyListState,
  modifier: Modifier = Modifier,
) {
  val tokens = LocalLiveGlass.current
  val list = sessions.orEmpty()
  val attention = list.count { it.needsAttention }
  val working = list.count {
    it.status == SessionStatus.WORKING ||
      it.status == SessionStatus.CONNECTING ||
      it.status == SessionStatus.RECONNECTING
  }
  val live = list.count {
    it.status == SessionStatus.LIVE ||
      it.status == SessionStatus.WATCHING
  }
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    state = listState,
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 132.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item(key = "summary", contentType = "summary") {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(LiveGlassShapes.Hero)
          .background(tokens.glass)
          .border(1.dp, tokens.edge, LiveGlassShapes.Hero),
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = if (attention > 0) {
              pluralStringResource(R.plurals.sessions_need_you, attention, attention)
            } else {
              stringResource(R.string.activity_nothing_needs_you)
            },
            style = MaterialTheme.typography.titleLarge,
            color = if (attention > 0) {
              LocalSessionStatusColors.current.waitingText
            } else {
              tokens.ink
            },
          )
          Spacer(Modifier.height(6.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = pluralStringResource(R.plurals.sessions_live, live, live),
              style = MaterialTheme.typography.bodyMedium,
              color = tokens.muted,
            )
            Text("·", style = MaterialTheme.typography.bodyMedium, color = tokens.muted.copy(alpha = 0.5f))
            Text(
              text = stringResource(R.string.activity_active, working),
              style = MaterialTheme.typography.bodyMedium,
              color = tokens.muted,
            )
            Text("·", style = MaterialTheme.typography.bodyMedium, color = tokens.muted.copy(alpha = 0.5f))
            Text(
              text = stringResource(R.string.sessions_stored, list.size),
              style = MaterialTheme.typography.bodyMedium,
              color = tokens.muted,
            )
          }
        }
      }
    }

    item(key = "now-header", contentType = "header") {
      ActivityHeader(stringResource(R.string.activity_now))
    }

    if (sessions == null) {
      item(key = "loading", contentType = "loading") {
        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }
    } else if (list.isEmpty()) {
      item(key = "empty", contentType = "empty") {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          shape = LiveGlassShapes.Panel,
          color = tokens.glass,
          border = androidx.compose.foundation.BorderStroke(1.dp, tokens.edge),
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.activity_empty_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
              stringResource(R.string.activity_empty_body),
              style = MaterialTheme.typography.bodyMedium,
              color = tokens.muted,
            )
          }
        }
      }
    } else {
      items(
        items = list.sortedWith(
          compareBy(
            { !it.needsAttention },
            { it.status != SessionStatus.WORKING },
            { it.status == SessionStatus.ENDED },
          ),
        ),
        key = { it.id },
        contentType = { "session" },
      ) { overview ->
        ActivitySessionRow(overview = overview, onClick = { onOpenSession(overview.id) })
      }
    }

    item(key = "recent-header", contentType = "header") {
      ActivityHeader(stringResource(R.string.activity_recent))
    }
    if (events.isEmpty()) {
      item(key = "no-events", contentType = "empty") {
        Text(
          text = stringResource(R.string.activity_no_events),
          style = MaterialTheme.typography.bodyMedium,
          color = tokens.muted,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
        )
      }
    } else {
      items(events, key = { it.id }, contentType = { "event" }) { event ->
        ActivityEventRow(event)
      }
    }
  }
}

@Composable
private fun ActivityHeader(text: String) {
  Text(
    text = text.uppercase(),
    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
    style = MaterialTheme.typography.labelMedium,
    color = LocalLiveGlass.current.muted,
    letterSpacing = 1.2.sp,
  )
}

@Composable
private fun ActivitySessionRow(
  overview: SessionOverview,
  onClick: () -> Unit,
) {
  val tokens = LocalLiveGlass.current
  val statusColors = LocalSessionStatusColors.current
  val context = LocalContext.current
  val relative = remember(overview.snapshot?.lastUpdated, overview.summary.lastActiveAt) {
    relativeTime(context, overview.snapshot?.lastUpdated ?: overview.summary.lastActiveAt)
  }
  val detail = overview.snapshot?.uiRequest?.title
    ?: overview.snapshot?.notices?.lastOrNull()?.text
    ?: overview.snapshot?.todos?.next?.content
    ?: overview.summary.cwd
    ?: overview.summary.roomId
  Surface(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    shape = LiveGlassShapes.Room,
    color = tokens.glass,
    border = androidx.compose.foundation.BorderStroke(1.dp, tokens.edge),
  ) {
    Row(
      modifier = Modifier.padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .background(statusColors.working.copy(alpha = 0.14f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = initials(overview.title),
          style = MaterialTheme.typography.labelMedium,
          color = statusColors.workingText,
        )
      }
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = overview.title,
            style = MaterialTheme.typography.titleSmall,
            color = tokens.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          StatusDot(status = overview.status)
          Text(
            text = statusLabel(overview.status),
            style = MaterialTheme.typography.labelSmall,
            color = statusTextColor(overview.status),
          )
        }
        Text(
          text = detail,
          style = MaterialTheme.typography.bodySmall,
          color = tokens.muted,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = relative,
          style = MaterialTheme.typography.labelSmall,
          color = tokens.muted.copy(alpha = 0.8f),
        )
      }
    }
  }
}

@Composable
private fun ActivityEventRow(event: SessionActivityEvent) {
  val tokens = LocalLiveGlass.current
  val statusColors = LocalSessionStatusColors.current
  val context = LocalContext.current
  val relative = remember(event.timestamp) { relativeTime(context, event.timestamp) }
  val text = when (event.kind) {
    ActivityEventKind.ADDED -> stringResource(R.string.activity_event_added)
    ActivityEventKind.REMOVED -> stringResource(R.string.activity_event_removed)
    ActivityEventKind.STATUS -> stringResource(
      R.string.activity_event_status,
      event.status?.let { statusLabel(it) }.orEmpty(),
    )
    ActivityEventKind.NEEDS_INPUT -> stringResource(R.string.activity_event_needs_input)
    ActivityEventKind.NOTICE -> stringResource(
      R.string.activity_event_notice,
      event.text.orEmpty(),
    )
    ActivityEventKind.TODO -> stringResource(R.string.activity_event_todo, event.text.orEmpty())
    ActivityEventKind.SUBAGENTS -> stringResource(R.string.activity_event_subagents, event.count ?: 1)
  }
  val dot = when (event.kind) {
    ActivityEventKind.NEEDS_INPUT -> statusColors.waiting
    ActivityEventKind.STATUS -> event.status?.let { statusColor(it) } ?: statusColors.working
    ActivityEventKind.NOTICE,
    ActivityEventKind.TODO,
    ActivityEventKind.SUBAGENTS,
    -> statusColors.working
    ActivityEventKind.ADDED -> statusColors.working
    ActivityEventKind.REMOVED -> statusColors.ended
  }
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Box(
      modifier = Modifier
        .padding(top = 5.dp)
        .size(8.dp)
        .background(dot, CircleShape),
    )
    Column(modifier = Modifier.weight(1f)) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = event.sessionTitle,
          style = MaterialTheme.typography.labelMedium,
          color = tokens.ink,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        Text(
          text = relative,
          style = MaterialTheme.typography.labelSmall,
          color = tokens.muted,
        )
      }
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = tokens.muted,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

private fun relativeTime(context: android.content.Context, timestamp: Long): String {
  val age = System.currentTimeMillis() - timestamp
  if (age < 60_000) return context.getString(R.string.sessions_just_now)
  return DateUtils.getRelativeTimeSpanString(
    timestamp,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
  ).toString()
}

private fun initials(title: String): String {
  val parts = title.split(Regex("[^A-Za-z0-9]+")).filter { it.isNotBlank() }
  if (parts.size >= 2) return (parts[0].take(1) + parts[1].take(1)).uppercase()
  return title.take(2).uppercase()
}
