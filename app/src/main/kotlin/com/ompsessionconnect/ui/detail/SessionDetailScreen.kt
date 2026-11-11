@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ompsessionconnect.ui.detail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ompsessionconnect.R
import com.ompsessionconnect.domain.model.AssistantMessageItem
import com.ompsessionconnect.domain.model.NoticeItem
import com.ompsessionconnect.domain.model.NoticeLevel
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionPhase
import com.ompsessionconnect.domain.model.SessionSnapshot
import com.ompsessionconnect.domain.model.SummaryItem
import com.ompsessionconnect.domain.model.ToolCallItem
import com.ompsessionconnect.domain.model.TodoProgress
import com.ompsessionconnect.domain.model.ToolStatus
import com.ompsessionconnect.domain.model.TranscriptItem
import com.ompsessionconnect.domain.model.UiRequest
import com.ompsessionconnect.domain.model.UiRequestKind
import com.ompsessionconnect.domain.model.UserMessageItem
import com.ompsessionconnect.ui.AppEvent
import com.ompsessionconnect.ui.AppViewModel
import com.ompsessionconnect.ui.components.ConfirmDialog
import com.ompsessionconnect.ui.components.StatusPill
import com.ompsessionconnect.ui.components.rememberVoiceInput
import com.ompsessionconnect.ui.components.statusLabelRes
import com.ompsessionconnect.ui.theme.CodeTextStyle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

@Composable
fun SessionDetailScreen(
  sessionId: String,
  sessions: List<SessionOverview>?,
  appViewModel: AppViewModel,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val overview = sessions?.firstOrNull { it.id == sessionId }

  LaunchedEffect(appViewModel) {
    appViewModel.events.collect { event ->
      val message = when (event) {
        is AppEvent.Error -> event.message ?: context.getString(R.string.error_generic)
        AppEvent.SessionClosed -> context.getString(R.string.session_closed)
        is AppEvent.ClosedEnded -> context.resources.getQuantityString(
          R.plurals.close_ended_count,
          event.count,
          event.count,
        )
      }
      snackbarHostState.showSnackbar(message)
    }
  }

  when {
    sessions == null -> {
      Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { DetailTopBar(title = stringResource(R.string.detail_title), onBack = onBack) },
      ) { padding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      }
    }
    overview == null -> {
      Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { DetailTopBar(title = stringResource(R.string.detail_no_session), onBack = onBack) },
      ) { padding ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(padding),
          contentAlignment = Alignment.Center,
        ) {
          Text(stringResource(R.string.detail_no_session))
        }
      }
    }
    else -> {
      SessionDetailContent(
        overview = overview,
        appViewModel = appViewModel,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
      )
    }
  }
}

@Composable
private fun SessionDetailContent(
  overview: SessionOverview,
  appViewModel: AppViewModel,
  snackbarHostState: SnackbarHostState,
  onBack: () -> Unit,
) {
  val snapshot = overview.snapshot
  val status = overview.status
  var menuOpen by remember { mutableStateOf(false) }
  var closeConfirm by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  var draft by rememberSaveable(overview.id) { mutableStateOf("") }
  var micGranted by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED,
    )
  }
  val voice = rememberVoiceInput(
    onText = { draft = it },
    onError = { code ->
      scope.launch { snackbarHostState.showSnackbar(voiceErrorMessage(context, code)) }
    },
  )
  val micPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { granted ->
    micGranted = granted
    if (granted) {
      voice.start(draft)
    } else {
      scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.voice_permission_denied)) }
    }
  }
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner, voice) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP) voice.stop()
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  fun onMicClick() {
    when {
      !voice.available -> scope.launch {
        snackbarHostState.showSnackbar(context.getString(R.string.voice_unavailable))
      }
      voice.listening -> voice.stop()
      micGranted -> voice.start(draft)
      else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  val transcript = remember(snapshot?.transcript) {
    snapshot?.transcript?.asReversed().orEmpty()
  }
  val streamText = snapshot?.streamingText
  val streamThinking = snapshot?.streamingThinking

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = overview.title,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              StatusPill(status = status)
              if (snapshot?.readOnly == true) {
                Icon(
                  Icons.Outlined.Lock,
                  contentDescription = stringResource(R.string.status_read_only),
                  modifier = Modifier.size(14.dp),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
          }
        },
        actions = {
          if (snapshot?.phase == SessionPhase.FAILED || snapshot?.phase == SessionPhase.ENDED) {
            IconButton(onClick = { appViewModel.reconnect(overview.id) }) {
              Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.action_reconnect))
            }
          }
          Box {
            IconButton(onClick = { menuOpen = true }) {
              Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
              DropdownMenuItem(
                text = { Text(stringResource(R.string.action_close_session)) },
                leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null) },
                onClick = {
                  menuOpen = false
                  closeConfirm = true
                },
              )
            }
          }
        },
      )
    },
    bottomBar = {
      if (snapshot != null) {
        Column(modifier = Modifier.imePadding()) {
          snapshot.todos?.let { todos ->
            if (todos.tasks.isNotEmpty()) {
              TodoStrip(todos)
            }
          }
          AnimatedVisibility(visible = voice.listening) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              color = MaterialTheme.colorScheme.tertiaryContainer,
              contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  painterResource(R.drawable.ic_mic),
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Text(stringResource(R.string.voice_listening), style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(
                  progress = { voice.rms },
                  modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { voice.stop() }) {
                  Text(stringResource(R.string.voice_stop))
                }
              }
            }
          }
          if (draft.startsWith("/")) {
            SessionCommandHint()
          }
          snapshot.uiRequest?.let { request ->
            AskPanel(
              request = request,
              onAnswer = { value ->
                appViewModel.respondToUi(overview.id, request.requestId, value)
              },
            )
          }
          Composer(
            enabled = snapshot.canPrompt,
            working = snapshot.working,
            readOnly = snapshot.readOnly,
            draft = draft,
            onDraftChange = { draft = it },
            onSend = {
              val text = draft.trim()
              if (text.isNotEmpty()) {
                draft = ""
                if (text.startsWith("/")) {
                  appViewModel.sendCommand(overview.id, text)
                } else {
                  appViewModel.sendPrompt(overview.id, text)
                  scope.launch { listState.animateScrollToItem(0) }
                }
              }
            },
            onStop = { appViewModel.abort(overview.id) },
            onVoice = ::onMicClick,
            listening = voice.listening,
            voiceEnabled = voice.available,
          )
        }
      }
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
    ) {
      snapshot?.let { state ->
        StateBanners(state = state, onReconnect = { appViewModel.reconnect(overview.id) })
        if (state.phase == SessionPhase.LIVE || state.phase == SessionPhase.CONNECTING) {
          SessionMeta(state)
        }
        state.notices.lastOrNull { it.level != NoticeLevel.INFO }?.let { notice ->
          if (System.currentTimeMillis() - notice.timestamp < 120_000) {
            Text(
              text = notice.text,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
              style = MaterialTheme.typography.labelSmall,
              color = if (notice.level == NoticeLevel.ERROR) {
                MaterialTheme.colorScheme.error
              } else {
                MaterialTheme.colorScheme.tertiary
              },
            )
          }
        }
      }

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        state = listState,
        reverseLayout = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item(key = "stream", contentType = "stream") {
          if (streamText != null || streamThinking != null) {
            AssistantBubble(
              text = streamText.orEmpty(),
              thinking = streamThinking.orEmpty(),
              streaming = snapshot?.working == true,
              model = snapshot?.modelName,
            )
          }
        }
        if (transcript.isEmpty() && streamText == null && streamThinking == null) {
          item(key = "empty", contentType = "empty") {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = stringResource(R.string.detail_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
        items(
          items = transcript,
          key = { it.id },
          contentType = { item -> item.itemType.name },
        ) { item ->
          TranscriptRow(item)
        }
      }
    }
  }

  if (closeConfirm) {
    ConfirmDialog(
      title = stringResource(R.string.detail_close_confirm_title),
      body = stringResource(R.string.detail_close_confirm_body),
      confirmLabel = stringResource(R.string.action_close_session),
      onConfirm = {
        closeConfirm = false
        appViewModel.close(overview.id)
        onBack()
      },
      onDismiss = { closeConfirm = false },
    )
  }
}

@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
  TopAppBar(
    title = { Text(title) },
    navigationIcon = {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
      }
    },
  )
}

@Composable
private fun StateBanners(state: SessionSnapshot, onReconnect: () -> Unit) {
  when (state.phase) {
    SessionPhase.CONNECTING -> {
      if (state.loadingTotal > 0) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          LinearProgressIndicator(
            progress = {
              (state.loadingReceived.toFloat() / state.loadingTotal.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth(),
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.status_syncing, state.loadingReceived, state.loadingTotal),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          Text(stringResource(R.string.detail_loading), style = MaterialTheme.typography.bodySmall)
        }
      }
    }
    SessionPhase.RECONNECTING -> {
      Banner(
        text = stringResource(R.string.detail_reconnecting_banner),
        container = MaterialTheme.colorScheme.tertiaryContainer,
        content = MaterialTheme.colorScheme.onTertiaryContainer,
        actionLabel = null,
        onAction = {},
      )
    }
    SessionPhase.FAILED -> {
      Banner(
        text = stringResource(
          R.string.detail_error_banner,
          state.endedReason ?: stringResource(R.string.error_generic),
        ),
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.onErrorContainer,
        actionLabel = stringResource(R.string.action_reconnect),
        onAction = onReconnect,
      )
    }
    SessionPhase.ENDED -> {
      Banner(
        text = stringResource(R.string.detail_ended_banner, state.endedReason ?: ""),
        container = MaterialTheme.colorScheme.surfaceContainerHighest,
        content = MaterialTheme.colorScheme.onSurface,
        actionLabel = stringResource(R.string.action_reconnect),
        onAction = onReconnect,
      )
    }
    SessionPhase.LIVE -> Unit
  }
}

@Composable
private fun Banner(
  text: String,
  container: Color,
  content: Color,
  actionLabel: String?,
  onAction: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = MaterialTheme.shapes.large,
    color = container,
    contentColor = content,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(Icons.Outlined.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
      Text(text = text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
      if (actionLabel != null) {
        TextButton(onClick = onAction) { Text(actionLabel) }
      }
    }
  }
}

@Composable
private fun SessionMeta(state: SessionSnapshot) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MetaChip(state.modelName ?: stringResource(R.string.detail_model_unknown))
    state.contextPercent?.let { MetaChip("$it%") }
    if (state.participantCount > 0) {
      MetaChip(
        pluralStringResource(
          R.plurals.participants,
          state.participantCount,
          state.participantCount,
        ),
      )
    }
    if (state.activeAgents > 0) {
      MetaChip(
        pluralStringResource(R.plurals.subagents, state.activeAgents, state.activeAgents),
      )
    }
  }
}

@Composable
private fun MetaChip(text: String) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      style = MaterialTheme.typography.labelSmall,
      maxLines = 1,
    )
  }
}

@Composable
private fun TranscriptRow(item: TranscriptItem) {
  when (item) {
    is UserMessageItem -> UserBubble(item)
    is AssistantMessageItem -> AssistantBubble(
      text = item.text,
      thinking = item.thinking,
      streaming = false,
      model = item.model,
    )
    is ToolCallItem -> ToolCard(item)
    is NoticeItem -> NoticeRow(item)
    is SummaryItem -> SummaryRow(item)
  }
}

@Composable
private fun UserBubble(item: UserMessageItem) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      modifier = Modifier.fillMaxWidth(0.86f),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
          text = item.from?.let { stringResource(R.string.transcript_guest, it) }
            ?: stringResource(R.string.transcript_you),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
        )
        if (item.text.isNotBlank()) {
          SelectionContainer {
            Text(text = item.text, style = MaterialTheme.typography.bodyMedium)
          }
        }
        if (item.imageCount > 0) {
          Spacer(Modifier.height(4.dp))
          Text(
            text = pluralStringResource(R.plurals.transcript_images, item.imageCount, item.imageCount),
            style = MaterialTheme.typography.labelSmall,
          )
        }
      }
    }
  }
}

@Composable
private fun AssistantBubble(
  text: String,
  thinking: String,
  streaming: Boolean,
  model: String?,
) {
  var thinkingOpen by remember { mutableStateOf(false) }
  Row(modifier = Modifier.fillMaxWidth()) {
    Surface(
      shape = MaterialTheme.shapes.large,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.fillMaxWidth(0.94f),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = stringResource(R.string.transcript_assistant),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
          )
          if (model != null) {
            Text(
              text = model,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (streaming) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
          }
        }
        if (thinking.isNotBlank()) {
          TextButton(
            onClick = { thinkingOpen = !thinkingOpen },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 2.dp),
          ) {
            Icon(
              if (thinkingOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.transcript_thinking), style = MaterialTheme.typography.labelSmall)
          }
          AnimatedVisibility(visible = thinkingOpen) {
            Text(
              text = thinking,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 4.dp),
            )
          }
        }
        if (text.isNotBlank()) {
          SelectionContainer {
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}

@Composable
private fun ToolCard(item: ToolCallItem) {
  var expanded by remember { mutableStateOf(false) }
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .animateContentSize(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
  ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            Icons.Outlined.Build,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.tertiary,
          )
          Text(
            text = item.toolName,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          when (item.status) {
            ToolStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            ToolStatus.DONE -> Icon(
              Icons.Outlined.CheckCircle,
              contentDescription = stringResource(R.string.tool_done),
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
            ToolStatus.ERROR -> Icon(
              Icons.Outlined.Warning,
              contentDescription = stringResource(R.string.tool_error),
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.error,
            )
          }
          IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
            Icon(
              if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
          }
        }
        if (item.arguments.isNotBlank()) {
          Spacer(Modifier.height(4.dp))
          Text(
            text = item.arguments,
            style = CodeTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (expanded && !item.output.isNullOrBlank()) {
          Spacer(Modifier.height(8.dp))
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
          ) {
            SelectionContainer {
              Text(
                text = item.output.orEmpty(),
                style = CodeTextStyle,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(8.dp),
              )
            }
          }
        }
      }
  }
}

@Composable
private fun NoticeRow(item: NoticeItem) {
  val color = when (item.level) {
    NoticeLevel.ERROR -> MaterialTheme.colorScheme.error
    NoticeLevel.WARNING -> MaterialTheme.colorScheme.tertiary
    NoticeLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
  }
  Text(
    text = item.text,
    style = MaterialTheme.typography.labelSmall,
    color = color,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp),
  )
}

@Composable
private fun SummaryRow(item: SummaryItem) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Text(
      text = item.text,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(10.dp),
    )
  }
}

@Composable
private fun Composer(
  enabled: Boolean,
  working: Boolean,
  readOnly: Boolean,
  draft: String,
  onDraftChange: (String) -> Unit,
  onSend: () -> Unit,
  onStop: () -> Unit,
  onVoice: () -> Unit,
  listening: Boolean,
  voiceEnabled: Boolean,
) {
  Surface(
    tonalElevation = 3.dp,
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = draft,
        onValueChange = onDraftChange,
        modifier = Modifier.weight(1f),
        enabled = enabled,
        placeholder = {
          Text(
            when {
              readOnly -> stringResource(R.string.detail_read_only_hint)
              !enabled -> stringResource(R.string.error_not_live)
              else -> stringResource(R.string.detail_composer_hint)
            },
          )
        },
        maxLines = 5,
        shape = MaterialTheme.shapes.extraLarge,
        trailingIcon = {
          IconButton(onClick = onVoice, enabled = enabled && voiceEnabled) {
            Icon(
              painterResource(R.drawable.ic_mic),
              contentDescription = stringResource(
                if (listening) R.string.voice_stop else R.string.voice_start,
              ),
              tint = if (listening) MaterialTheme.colorScheme.error else LocalContentColor.current,
            )
          }
        },
      )
      if (working && enabled) {
        IconButton(
          onClick = onStop,
          modifier = Modifier
            .size(48.dp)
            .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
        ) {
          Icon(
            Icons.Outlined.Clear,
            contentDescription = stringResource(R.string.action_stop),
            tint = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
      }
      IconButton(
        onClick = onSend,
        enabled = enabled && draft.isNotBlank(),
        modifier = Modifier.size(48.dp),
      ) {
        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.action_send))
      }
    }
  }
}

@Composable
private fun TodoStrip(todos: TodoProgress) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${todos.completed}/${todos.total}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        LinearProgressIndicator(
          progress = { if (todos.total == 0) 0f else todos.completed.toFloat() / todos.total },
          modifier = Modifier.weight(1f),
        )
      }
      todos.next?.content?.let { current ->
        Text(
          text = current,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun SessionCommandHint() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text("/", style = CodeTextStyle)
      Text(
        text = stringResource(R.string.session_command_hint),
        style = MaterialTheme.typography.labelMedium,
      )
    }
  }
}

private fun voiceErrorMessage(context: Context, code: Int): String = context.getString(
  when (code) {
    SpeechRecognizer.ERROR_NO_MATCH,
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
    -> R.string.voice_error_no_match
    SpeechRecognizer.ERROR_NETWORK,
    SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
    SpeechRecognizer.ERROR_SERVER,
    -> R.string.voice_error_network
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.voice_error_permission
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS,
    -> R.string.voice_error_busy
    else -> R.string.voice_error_generic
  },
)

@Composable
private fun AskPanel(
  request: UiRequest,
  onAnswer: (String?) -> Unit,
) {
  var editor by remember(request.requestId) { mutableStateOf(request.prefill.orEmpty()) }
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(text = request.title, style = MaterialTheme.typography.titleSmall)
      request.helpText?.let {
        Text(text = it, style = MaterialTheme.typography.bodySmall)
      }
      Spacer(Modifier.height(8.dp))
      when (request.kind) {
        UiRequestKind.SELECT -> {
          request.options.forEachIndexed { index, option ->
            val checked = index in request.checkedIndices
            OutlinedButton(
              onClick = { onAnswer(option.label) },
              colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            ) {
              Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                  text = (if (checked) "✓ " else "") + option.label,
                  style = MaterialTheme.typography.bodyMedium,
                )
                option.description?.let {
                  Text(text = it, style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
          TextButton(
            onClick = { onAnswer(null) },
            colors = ButtonDefaults.textButtonColors(
              contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
          ) {
            Text(stringResource(R.string.prompt_cancel))
          }
        }
        UiRequestKind.EDITOR -> {
          OutlinedTextField(
            value = editor,
            onValueChange = { editor = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.prompt_editor_hint)) },
            maxLines = 5,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(
              onClick = { onAnswer(null) },
              colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
              ),
            ) {
              Text(stringResource(R.string.prompt_cancel))
            }
            Button(onClick = { onAnswer(editor) }) {
              Text(stringResource(R.string.prompt_send))
            }
          }
        }
      }
    }
  }
}
