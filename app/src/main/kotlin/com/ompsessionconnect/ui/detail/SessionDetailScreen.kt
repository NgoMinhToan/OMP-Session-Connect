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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import android.os.Build
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.ompsessionconnect.domain.model.SubagentInfo
import com.ompsessionconnect.domain.model.SubagentStatus
import com.ompsessionconnect.domain.model.TodoProgress
import com.ompsessionconnect.domain.model.TodoStatus
import com.ompsessionconnect.domain.model.ToolStatus
import com.ompsessionconnect.domain.model.TranscriptItem
import com.ompsessionconnect.domain.model.UiRequest
import com.ompsessionconnect.domain.model.UiRequestKind
import com.ompsessionconnect.domain.model.UserMessageItem
import com.ompsessionconnect.ui.AppEvent
import com.ompsessionconnect.ui.AppViewModel
import com.ompsessionconnect.ui.components.ConfirmDialog
import com.ompsessionconnect.ui.components.edgeFade
import com.ompsessionconnect.ui.components.GlassIconButton
import com.ompsessionconnect.ui.components.StatusPill
import com.ompsessionconnect.ui.components.rememberVoiceInput
import com.ompsessionconnect.ui.components.statusLabelRes
import com.ompsessionconnect.ui.theme.CodeMediumTextStyle
import com.ompsessionconnect.ui.theme.CodeTextStyle
import com.ompsessionconnect.ui.theme.LiveGlassShapes
import com.ompsessionconnect.ui.theme.LocalLiveGlass
import com.ompsessionconnect.ui.theme.LocalSessionStatusColors
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
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
  val resources = LocalResources.current
  val genericError = stringResource(R.string.error_generic)
  val sessionClosedText = stringResource(R.string.session_closed)
  val snackbarHostState = remember { SnackbarHostState() }
  val overview = sessions?.firstOrNull { it.id == sessionId }

  LaunchedEffect(appViewModel) {
    appViewModel.events.collect { event ->
      val message = when (event) {
        is AppEvent.Error -> event.message ?: genericError
        AppEvent.SessionClosed -> sessionClosedText
        is AppEvent.ClosedEnded -> resources.getQuantityString(
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
        containerColor = Color.Transparent,
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
        containerColor = Color.Transparent,
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

private val ChromeFade = 36.dp

private fun chromeBlurStyle(field: Color, dark: Boolean): HazeBlurStyle = HazeBlurStyle {
  blurRadius(24.dp)
  backgroundColor(field)
  colorEffects(
    listOf(
      HazeColorEffect.tint(field.copy(alpha = if (dark) 0.45f else 0.35f)),
    ),
  )
  blurredEdgeTreatment(BlurredEdgeTreatment.Rectangle)
  noiseFactor(0f)
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
  val tokens = LocalLiveGlass.current
  var menuOpen by remember { mutableStateOf(false) }
  var closeConfirm by remember { mutableStateOf(false) }
  var subagentsOpen by remember { mutableStateOf(false) }
  var selectedAgent by remember { mutableStateOf<SubagentInfo?>(null) }
  var agentItems by remember { mutableStateOf<List<TranscriptItem>>(emptyList()) }
  var agentLoading by remember { mutableStateOf(false) }
  var agentError by remember { mutableStateOf<String?>(null) }
  var todoSheetOpen by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val hazeState = rememberHazeState()
  var topChromeHeightPx by remember { mutableStateOf(0) }
  var bottomChromeHeightPx by remember { mutableStateOf(0) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val voicePermissionDeniedText = stringResource(R.string.voice_permission_denied)
  val voiceUnavailableText = stringResource(R.string.voice_unavailable)
  val subagentTranscriptErrorText = stringResource(R.string.subagent_transcript_error)
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
      scope.launch { snackbarHostState.showSnackbar(voicePermissionDeniedText) }
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

  fun loadAgentTranscript(agent: SubagentInfo) {
    selectedAgent = agent
    agentItems = emptyList()
    agentError = null
    agentLoading = true
    scope.launch {
      val collected = mutableListOf<TranscriptItem>()
      val seenIds = mutableSetOf<String>()
      var offset = 0L
      var pages = 0
      while (pages < 5) {
        val result = appViewModel.fetchTranscript(overview.id, agent.id, offset)
        if (result.isFailure) {
          agentError = result.exceptionOrNull()?.message
            ?: subagentTranscriptErrorText
          break
        }
        val page = result.getOrThrow()
        page.items.forEach { item ->
          if (seenIds.add(item.id)) collected.add(item)
        }
        if (page.items.isEmpty() || page.nextOffset <= offset) break
        offset = page.nextOffset
        pages++
      }
      agentItems = collected
      agentLoading = false
    }
  }

  fun onMicClick() {
    when {
      !voice.available -> scope.launch {
        snackbarHostState.showSnackbar(voiceUnavailableText)
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

  val blurAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  val legacyInsets = !blurAvailable
  val density = LocalDensity.current
  val topContentInset = if (blurAvailable && topChromeHeightPx > 0) {
    with(density) { topChromeHeightPx.toDp() } + 8.dp
  } else {
    16.dp
  }
  val bottomContentInset = if (blurAvailable && bottomChromeHeightPx > 0) {
    with(density) { bottomChromeHeightPx.toDp() } + 8.dp
  } else {
    16.dp
  }
  val topBlurStyle = remember(tokens.fieldA, tokens.dark) {
    chromeBlurStyle(tokens.fieldA, tokens.dark)
  }
  val bottomBlurStyle = remember(tokens.fieldC, tokens.dark) {
    chromeBlurStyle(tokens.fieldC, tokens.dark)
  }
  fun chromeBlur(style: HazeBlurStyle): Modifier =
    if (blurAvailable) {
      Modifier.hazeBlur(
        input = HazeInput.Sources(hazeState),
        style = style,
      )
    } else {
      Modifier
    }

  Box(modifier = Modifier.fillMaxSize()) {
    // Keep the transcript in its own Haze source layer; the fixed chrome effects below
    // sample this layer and blur only the pixels inside each chrome box.
    Box(
      modifier = Modifier
        .fillMaxSize()
        .then(if (blurAvailable) Modifier.hazeSource(hazeState) else Modifier),
    ) {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .edgeFade(top = 18.dp, bottom = 26.dp),
        state = listState,
        reverseLayout = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
          start = 16.dp,
          end = 16.dp,
          top = if (legacyInsets) 200.dp else topContentInset,
          bottom = if (legacyInsets) 140.dp else bottomContentInset,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        item(key = "stream", contentType = "stream") {
          if (streamText != null || streamThinking != null) {
            AssistantBubble(
              text = streamText.orEmpty(),
              thinking = streamThinking.orEmpty(),
              streaming = snapshot.working,
              model = snapshot.modelName,
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

    Box(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .zIndex(3f)
        .then(chromeBlur(topBlurStyle))
        .onSizeChanged { topChromeHeightPx = it.height }
        .then(
          if (blurAvailable) {
            Modifier
          } else {
            Modifier.background(tokens.control.copy(alpha = 0.58f))
          },
        ),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .zIndex(2f)
          .statusBarsPadding()
          .padding(top = 8.dp, bottom = ChromeFade),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          GlassIconButton(
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            onClick = onBack,
          )
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 10.dp),
          ) {
            Text(
              text = overview.title,
              style = MaterialTheme.typography.titleSmall,
              color = tokens.ink,
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
                  tint = tokens.muted,
                )
              }
            }
          }
          if (snapshot?.phase == SessionPhase.FAILED || snapshot?.phase == SessionPhase.ENDED) {
            GlassIconButton(
              icon = Icons.Outlined.Refresh,
              contentDescription = stringResource(R.string.action_reconnect),
              onClick = { appViewModel.reconnect(overview.id) },
            )
          }
          Box {
            GlassIconButton(
              icon = Icons.Outlined.MoreVert,
              contentDescription = stringResource(R.string.action_more),
              onClick = { menuOpen = true },
            )
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
        }
        snapshot?.let { state ->
          StateBanners(state = state, onReconnect = { appViewModel.reconnect(overview.id) })
          if (state.phase == SessionPhase.LIVE || state.phase == SessionPhase.CONNECTING) {
            SessionMeta(state)
            if (state.subagents.isNotEmpty()) {
              SubagentBar(
                agents = state.subagents,
                onClick = { subagentsOpen = true },
              )
            }
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
      }
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .height(ChromeFade)
          .zIndex(3f)
          .background(
            Brush.verticalGradient(
              colorStops = arrayOf(
                0f to tokens.fieldA,
                0.55f to tokens.fieldA.copy(alpha = 0.55f),
                1f to Color.Transparent,
              ),
            ),
          ),
      )
    }

    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .zIndex(3f)
        .then(chromeBlur(bottomBlurStyle))
        .onSizeChanged { bottomChromeHeightPx = it.height },
    ) {
      if (snapshot != null) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .zIndex(2f)
            .imePadding()
            .navigationBarsPadding()
            .padding(start = 16.dp, top = ChromeFade, end = 16.dp, bottom = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          snapshot.todos?.let { todos ->
            if (todos.tasks.isNotEmpty()) {
              TodoStrip(todos, onClick = { todoSheetOpen = true })
            }
          }
          AnimatedVisibility(visible = voice.listening) {
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = LiveGlassShapes.Room,
              color = tokens.glass,
              contentColor = tokens.ink,
              border = BorderStroke(1.dp, tokens.edge),
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
                  tint = tokens.ink,
                )
                Text(stringResource(R.string.voice_listening), style = MaterialTheme.typography.labelMedium)
                LinearProgressIndicator(
                  progress = { voice.rms },
                  modifier = Modifier.weight(1f),
                  color = tokens.primary,
                  trackColor = tokens.hairline,
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
      Box(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth()
          .height(ChromeFade)
          .zIndex(3f)
          .background(
            Brush.verticalGradient(
              colorStops = arrayOf(
                0f to Color.Transparent,
                0.45f to tokens.fieldC.copy(alpha = 0.55f),
                1f to tokens.fieldC,
              ),
            ),
          ),
      )
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .zIndex(4f)
        .padding(bottom = 96.dp),
    )
  }

  val agents = snapshot?.subagents.orEmpty()
  if (subagentsOpen) {
    SubagentsSheet(
      agents = agents,
      onDismiss = { subagentsOpen = false },
      onView = { agent ->
        subagentsOpen = false
        loadAgentTranscript(agent)
      },
    )
  }
  selectedAgent?.let { agent ->
    AgentTranscriptDialog(
      agent = agent,
      items = agentItems,
      loading = agentLoading,
      error = agentError,
      onRefresh = { loadAgentTranscript(agent) },
      onDismiss = {
        selectedAgent = null
        agentItems = emptyList()
        agentError = null
      },
    )
  }
  val todoProgress = snapshot?.todos
  if (todoSheetOpen && todoProgress != null) {
    TodoSheet(todos = todoProgress, onDismiss = { todoSheetOpen = false })
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
  val tokens = LocalLiveGlass.current
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    GlassIconButton(
      icon = Icons.AutoMirrored.Outlined.ArrowBack,
      contentDescription = stringResource(R.string.action_back),
      onClick = onBack,
    )
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = tokens.ink,
    )
  }
}

@Composable
private fun StateBanners(state: SessionSnapshot, onReconnect: () -> Unit) {
  val tokens = LocalLiveGlass.current
  val status = LocalSessionStatusColors.current
  when (state.phase) {
    SessionPhase.CONNECTING -> {
      if (state.loadingTotal > 0) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          LinearProgressIndicator(
            progress = {
              (state.loadingReceived.toFloat() / state.loadingTotal.toFloat()).coerceIn(0f, 1f)
            },
            modifier = Modifier.fillMaxWidth(),
            color = tokens.primary,
            trackColor = tokens.hairline,
          )
          Spacer(Modifier.height(4.dp))
          Text(
            text = stringResource(R.string.status_syncing, state.loadingReceived, state.loadingTotal),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.muted,
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
        container = status.working.copy(alpha = 0.14f),
        content = status.workingText,
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
        container = status.error.copy(alpha = 0.14f),
        content = status.errorText,
        actionLabel = stringResource(R.string.action_reconnect),
        onAction = onReconnect,
      )
    }
    SessionPhase.ENDED -> {
      Banner(
        text = stringResource(R.string.detail_ended_banner, state.endedReason ?: ""),
        container = tokens.glass,
        content = tokens.muted,
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
  val tokens = LocalLiveGlass.current
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    shape = LiveGlassShapes.Room,
    color = container,
    contentColor = content,
    border = BorderStroke(1.dp, tokens.edge),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionMeta(state: SessionSnapshot) {
  FlowRow(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(5.dp),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    MetaChip(state.modelName ?: stringResource(R.string.detail_model_unknown))
    state.contextPercent?.let {
      MetaChip(stringResource(R.string.sessions_context_percent, it))
    }
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
  val tokens = LocalLiveGlass.current
  Surface(
    shape = LiveGlassShapes.Pill,
    color = tokens.glassInset,
    contentColor = tokens.muted,
    border = BorderStroke(1.dp, tokens.hairline),
  ) {
    Text(
      text = text,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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
  val tokens = LocalLiveGlass.current
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
    Surface(
      shape = LiveGlassShapes.Hero,
      color = tokens.primary.copy(alpha = 0.13f),
      contentColor = tokens.ink,
      border = BorderStroke(1.dp, tokens.edge),
      modifier = Modifier.fillMaxWidth(0.86f),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(
          text = item.from?.let { stringResource(R.string.transcript_guest, it) }
            ?: stringResource(R.string.transcript_you),
          style = MaterialTheme.typography.labelSmall,
          color = tokens.muted,
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
            color = tokens.muted,
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
  val tokens = LocalLiveGlass.current
  Row(modifier = Modifier.fillMaxWidth()) {
    Surface(
      shape = LiveGlassShapes.Hero,
      color = tokens.glass,
      contentColor = tokens.ink,
      border = BorderStroke(1.dp, tokens.edge),
      modifier = Modifier.fillMaxWidth(0.94f),
    ) {
      Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = stringResource(R.string.transcript_assistant),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = tokens.primary,
          )
          if (model != null) {
            Text(
              text = model,
              style = CodeTextStyle,
              color = tokens.muted,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (streaming) {
            CircularProgressIndicator(
              modifier = Modifier.size(12.dp),
              strokeWidth = 2.dp,
              color = tokens.primary,
            )
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
            Text(
              stringResource(R.string.transcript_thinking),
              style = MaterialTheme.typography.labelSmall,
              color = tokens.muted,
            )
          }
          AnimatedVisibility(visible = thinkingOpen) {
            Text(
              text = thinking,
              style = MaterialTheme.typography.bodySmall,
              color = tokens.muted,
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
  val tokens = LocalLiveGlass.current
  val status = LocalSessionStatusColors.current
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .animateContentSize()
      .clickable { expanded = !expanded },
    shape = LiveGlassShapes.Room,
    color = tokens.glass,
    contentColor = tokens.ink,
    border = BorderStroke(1.dp, tokens.edge),
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
          tint = tokens.muted,
        )
        Text(
          text = item.toolName,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        when (item.status) {
          ToolStatus.RUNNING -> CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = status.working,
          )
          ToolStatus.DONE -> Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = stringResource(R.string.tool_done),
            modifier = Modifier.size(16.dp),
            tint = status.live,
          )
          ToolStatus.ERROR -> Icon(
            Icons.Outlined.Warning,
            contentDescription = stringResource(R.string.tool_error),
            modifier = Modifier.size(16.dp),
            tint = status.error,
          )
        }
        Icon(
          if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = tokens.muted,
        )
      }
      if (item.arguments.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(
          text = item.arguments,
          style = CodeTextStyle,
          color = tokens.muted,
          maxLines = if (expanded) Int.MAX_VALUE else 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (expanded && !item.output.isNullOrBlank()) {
        Spacer(Modifier.height(8.dp))
        Surface(
          shape = MaterialTheme.shapes.small,
          color = Color.Transparent,
          border = BorderStroke(1.dp, tokens.hairline),
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
  val status = LocalSessionStatusColors.current
  val tokens = LocalLiveGlass.current
  val color = when (item.level) {
    NoticeLevel.ERROR -> status.errorText
    NoticeLevel.WARNING -> status.waitingText
    NoticeLevel.INFO -> tokens.muted
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
  val tokens = LocalLiveGlass.current
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = LiveGlassShapes.Room,
    color = tokens.glassInset,
    border = BorderStroke(1.dp, tokens.hairline),
  ) {
    Text(
      text = item.text,
      style = MaterialTheme.typography.labelSmall,
      color = tokens.muted,
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
  val tokens = LocalLiveGlass.current
  val status = LocalSessionStatusColors.current
  val composerShape = LiveGlassShapes.Composer
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(composerShape)
      .background(tokens.control),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 7.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      BasicTextField(
        value = draft,
        onValueChange = onDraftChange,
        enabled = enabled,
        modifier = Modifier
          .weight(1f)
          .padding(horizontal = 8.dp, vertical = 10.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
          color = if (enabled) tokens.ink else tokens.muted,
        ),
        cursorBrush = SolidColor(tokens.primary),
        maxLines = 5,
        decorationBox = { innerTextField ->
          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
          ) {
            if (draft.isEmpty()) {
              Text(
                text = when {
                  readOnly -> stringResource(R.string.detail_read_only_hint)
                  !enabled -> stringResource(R.string.error_not_live)
                  else -> stringResource(R.string.detail_composer_hint)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            innerTextField()
          }
        },
      )
      IconButton(onClick = onVoice, enabled = enabled && voiceEnabled) {
        Icon(
          painterResource(R.drawable.ic_mic),
          contentDescription = stringResource(
            if (listening) R.string.voice_stop else R.string.voice_start,
          ),
          tint = if (listening) status.error else tokens.muted,
        )
      }
      if (working && enabled) {
        IconButton(
          onClick = onStop,
          modifier = Modifier
            .size(48.dp)
            .background(status.error.copy(alpha = 0.16f), CircleShape),
        ) {
          Icon(
            Icons.Outlined.Clear,
            contentDescription = stringResource(R.string.action_stop),
            tint = status.errorText,
          )
        }
      }
      IconButton(
        onClick = onSend,
        enabled = enabled && draft.isNotBlank(),
        modifier = Modifier
          .size(48.dp)
          .background(
            if (enabled && draft.isNotBlank()) tokens.primary else tokens.hairline,
            CircleShape,
          ),
      ) {
        Icon(
          Icons.AutoMirrored.Outlined.Send,
          contentDescription = stringResource(R.string.action_send),
          tint = if (enabled && draft.isNotBlank()) tokens.onPrimary else tokens.muted,
        )
      }
    }
  }
}

@Composable
private fun SubagentBar(
  agents: List<SubagentInfo>,
  onClick: () -> Unit,
) {
  val tokens = LocalLiveGlass.current
  val status = LocalSessionStatusColors.current
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 2.dp)
      .clickable(onClick = onClick),
    shape = LiveGlassShapes.Room,
    color = tokens.glass,
    border = BorderStroke(1.dp, tokens.edge),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        agents.take(2).forEach { agent ->
          val color = if (agent.status == SubagentStatus.RUNNING) status.working else status.ended
          Box(
            modifier = Modifier
              .size(26.dp)
              .background(color.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = agent.displayName.take(2).uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = color,
            )
          }
        }
      }
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.subagents_title),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = tokens.ink,
        )
        Text(
          text = stringResource(
            R.string.subagent_summary,
            agents.count { it.status == SubagentStatus.RUNNING },
            agents.size,
          ),
          style = MaterialTheme.typography.labelSmall,
          color = tokens.muted,
        )
      }
      Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = tokens.muted,
      )
    }
  }
}

@Composable
private fun SubagentsSheet(
  agents: List<SubagentInfo>,
  onDismiss: () -> Unit,
  onView: (SubagentInfo) -> Unit,
) {
  val tokens = LocalLiveGlass.current
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    shape = LiveGlassShapes.Sheet,
    containerColor = tokens.sheetOpaque,
    contentColor = tokens.ink,
  ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = stringResource(R.string.subagents_title),
          style = MaterialTheme.typography.titleMedium,
          color = tokens.ink,
        )
        Text(
          text = stringResource(
            R.string.subagent_summary,
            agents.count { it.status == SubagentStatus.RUNNING },
            agents.size,
          ),
          style = MaterialTheme.typography.labelSmall,
          color = tokens.muted,
        )
      }
      Spacer(Modifier.height(12.dp))
      if (agents.isEmpty()) {
        Text(
          text = stringResource(R.string.subagent_transcript_empty),
          style = MaterialTheme.typography.bodySmall,
          color = tokens.muted,
          modifier = Modifier.padding(vertical = 16.dp),
        )
      } else {
        LazyColumn(
          modifier = Modifier.heightIn(max = 520.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(agents, key = { it.id }, contentType = { "subagent" }) { agent ->
            val color = subagentStatusColor(agent.status)
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = agent.hasSessionFile) { onView(agent) },
              shape = LiveGlassShapes.Room,
              color = Color.Transparent,
              border = BorderStroke(1.dp, tokens.edge),
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.14f), CircleShape),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = agent.displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                  )
                }
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = agent.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.ink,
                  )
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                  ) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape),
                    )
                    Text(
                      text = stringResource(subagentStatusRes(agent.status)),
                      style = MaterialTheme.typography.labelSmall,
                      color = tokens.muted,
                    )
                  }
                }
                if (agent.hasSessionFile) {
                  TextButton(onClick = { onView(agent) }) {
                    Text(
                      text = stringResource(R.string.subagent_view),
                      color = tokens.primary,
                    )
                  }
                } else {
                  Text(
                    text = stringResource(R.string.subagent_no_log),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.muted,
                  )
                }
              }
            }
          }
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun AgentTranscriptDialog(
  agent: SubagentInfo,
  items: List<TranscriptItem>,
  loading: Boolean,
  error: String?,
  onRefresh: () -> Unit,
  onDismiss: () -> Unit,
) {
  val tokens = LocalLiveGlass.current
  val color = subagentStatusColor(agent.status)
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp)
        .fillMaxHeight(0.86f),
      shape = LiveGlassShapes.Dialog,
      color = tokens.sheetOpaque,
      contentColor = tokens.ink,
      border = BorderStroke(1.dp, tokens.edge),
      shadowElevation = 24.dp,
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = agent.displayName,
              style = MaterialTheme.typography.titleMedium,
              color = tokens.ink,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(color, CircleShape),
              )
              Text(
                text = stringResource(subagentStatusRes(agent.status)),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.muted,
              )
            }
          }
        }
        HorizontalDivider(color = tokens.hairline)
        if (loading) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(),
            color = tokens.primary,
            trackColor = tokens.hairline,
          )
        }
        error?.let {
          Text(
            text = it,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = LocalSessionStatusColors.current.errorText,
          )
        }
        if (items.isEmpty() && !loading && error == null) {
          Text(
            text = stringResource(R.string.subagent_transcript_empty),
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodySmall,
            color = tokens.muted,
          )
        } else {
          LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(items, key = { it.id }, contentType = { it.itemType.name }) { item ->
              TranscriptRow(item)
            }
          }
        }
        HorizontalDivider(color = tokens.hairline)
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(
            onClick = onRefresh,
            enabled = !loading,
            colors = ButtonDefaults.textButtonColors(contentColor = tokens.primary),
          ) {
            Text(stringResource(R.string.subagent_transcript_refresh))
          }
          Spacer(Modifier.width(6.dp))
          Button(
            onClick = onDismiss,
            shape = LiveGlassShapes.Pill,
            colors = ButtonDefaults.buttonColors(
              containerColor = tokens.primary,
              contentColor = tokens.onPrimary,
            ),
          ) {
            Text(stringResource(R.string.action_close))
          }
        }
      }
    }
  }
}

@Composable
private fun subagentStatusRes(status: SubagentStatus): Int = when (status) {
  SubagentStatus.RUNNING -> R.string.subagent_status_running
  SubagentStatus.IDLE -> R.string.subagent_status_idle
  SubagentStatus.PARKED -> R.string.subagent_status_parked
  SubagentStatus.ABORTED -> R.string.subagent_status_aborted
}

@Composable
private fun subagentStatusColor(status: SubagentStatus): Color {
  val colors = LocalSessionStatusColors.current
  return when (status) {
    SubagentStatus.RUNNING -> colors.working
    SubagentStatus.IDLE -> colors.ended
    SubagentStatus.PARKED -> colors.working
    SubagentStatus.ABORTED -> colors.error
  }
}

@Composable
private fun TodoSheet(todos: TodoProgress, onDismiss: () -> Unit) {
  val tokens = LocalLiveGlass.current
  val colors = LocalSessionStatusColors.current
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    shape = LiveGlassShapes.Sheet,
    containerColor = tokens.sheetOpaque,
    contentColor = tokens.ink,
  ) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = stringResource(R.string.todo_all_title),
          style = MaterialTheme.typography.titleMedium,
          color = tokens.ink,
        )
        Surface(
          shape = LiveGlassShapes.Pill,
          color = tokens.primary.copy(alpha = 0.13f),
          contentColor = colors.workingText,
        ) {
          Text(
            text = "${todos.completed}/${todos.total}",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
      Spacer(Modifier.height(10.dp))
      LazyColumn(
        modifier = Modifier.heightIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(
          items = todos.tasks,
          key = { "${it.phase}|${it.content}" },
          contentType = { "todo" },
        ) { task ->
          val color = todoStatusColor(task.status)
          val completed = task.status == TodoStatus.COMPLETED ||
            task.status == TodoStatus.ABANDONED
          Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = LiveGlassShapes.Room,
            color = Color.Transparent,
            border = BorderStroke(
              1.dp,
              if (task.status == TodoStatus.BLOCKED) {
                colors.blocked.copy(alpha = 0.45f)
              } else {
                tokens.edge
              },
            ),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (task.status == TodoStatus.COMPLETED) {
                Icon(
                  Icons.Outlined.CheckCircle,
                  contentDescription = stringResource(R.string.todo_status_completed),
                  modifier = Modifier.size(14.dp),
                  tint = color,
                )
              } else {
                Box(
                  modifier = Modifier
                    .size(9.dp)
                    .background(
                      if (task.status == TodoStatus.PENDING) {
                        color.copy(alpha = 0.5f)
                      } else {
                        color
                      },
                      CircleShape,
                    ),
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = task.content,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.SemiBold,
                  textDecoration = if (completed) TextDecoration.LineThrough else null,
                  color = if (completed) tokens.muted else tokens.ink,
                )
                Text(
                  text = task.phase,
                  style = MaterialTheme.typography.labelSmall,
                  color = tokens.muted,
                )
                task.blocker?.let {
                  Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.blocked,
                  )
                }
              }
              Text(
                text = todoStatusLabel(task.status),
                style = MaterialTheme.typography.labelSmall,
                color = color,
              )
            }
          }
        }
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
private fun todoStatusColor(status: TodoStatus): Color {
  val colors = LocalSessionStatusColors.current
  return when (status) {
    TodoStatus.COMPLETED -> colors.completed
    TodoStatus.IN_PROGRESS -> colors.progress
    TodoStatus.BLOCKED -> colors.blocked
    TodoStatus.ABANDONED -> colors.ended
    TodoStatus.PENDING -> colors.pending
  }
}

@Composable
private fun todoStatusLabel(status: TodoStatus): String = stringResource(
  when (status) {
    TodoStatus.PENDING -> R.string.todo_status_pending
    TodoStatus.IN_PROGRESS -> R.string.todo_status_in_progress
    TodoStatus.COMPLETED -> R.string.todo_status_completed
    TodoStatus.ABANDONED -> R.string.todo_status_abandoned
    TodoStatus.BLOCKED -> R.string.todo_status_blocked
  },
)

@Composable
private fun TodoStrip(todos: TodoProgress, onClick: () -> Unit) {
  val tokens = LocalLiveGlass.current
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = LiveGlassShapes.Room,
    color = tokens.glass,
    border = BorderStroke(1.dp, tokens.edge),
  ) {
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "${todos.completed}/${todos.total}",
          style = CodeMediumTextStyle,
          color = tokens.primary,
        )
        LinearProgressIndicator(
          progress = { if (todos.total == 0) 0f else todos.completed.toFloat() / todos.total },
          modifier = Modifier.weight(1f),
          color = tokens.primary,
          trackColor = tokens.hairline,
        )
        Icon(
          Icons.AutoMirrored.Outlined.KeyboardArrowRight,
          contentDescription = stringResource(R.string.todo_view_all),
          modifier = Modifier.size(18.dp),
          tint = tokens.muted,
        )
      }
      todos.next?.content?.let { current ->
        Text(
          text = current,
          style = MaterialTheme.typography.bodyMedium,
          color = tokens.ink,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun SessionCommandHint() {
  val tokens = LocalLiveGlass.current
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = LiveGlassShapes.Room,
    color = tokens.primary.copy(alpha = 0.12f),
    contentColor = tokens.ink,
    border = BorderStroke(1.dp, tokens.edge),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text("/", style = CodeMediumTextStyle, color = tokens.primary)
      Text(
        text = stringResource(R.string.session_command_hint),
        style = MaterialTheme.typography.labelMedium,
        color = tokens.ink,
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
  val tokens = LocalLiveGlass.current
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = LiveGlassShapes.Hero,
    color = tokens.glass,
    contentColor = tokens.ink,
    border = BorderStroke(1.dp, tokens.edge),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = request.title,
        style = MaterialTheme.typography.titleSmall,
        color = tokens.ink,
      )
      request.helpText?.let {
        Text(text = it, style = MaterialTheme.typography.bodySmall, color = tokens.muted)
      }
      Spacer(Modifier.height(8.dp))
      when (request.kind) {
        UiRequestKind.SELECT -> {
          request.options.forEachIndexed { index, option ->
            val checked = index in request.checkedIndices
            OutlinedButton(
              onClick = { onAnswer(option.label) },
              shape = LiveGlassShapes.Room,
              border = BorderStroke(1.dp, tokens.edge),
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = tokens.ink,
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
                  Text(text = it, style = MaterialTheme.typography.labelSmall, color = tokens.muted)
                }
              }
            }
          }
          TextButton(
            onClick = { onAnswer(null) },
            colors = ButtonDefaults.textButtonColors(contentColor = tokens.muted),
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
            shape = LiveGlassShapes.Room,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = tokens.ink,
              unfocusedTextColor = tokens.ink,
              focusedContainerColor = Color.Transparent,
              unfocusedContainerColor = Color.Transparent,
              focusedBorderColor = tokens.primary.copy(alpha = 0.7f),
              unfocusedBorderColor = tokens.edge,
              cursorColor = tokens.primary,
            ),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(
              onClick = { onAnswer(null) },
              colors = ButtonDefaults.textButtonColors(contentColor = tokens.muted),
            ) {
              Text(stringResource(R.string.prompt_cancel))
            }
            Button(
              onClick = { onAnswer(editor) },
              shape = LiveGlassShapes.Pill,
              colors = ButtonDefaults.buttonColors(
                containerColor = tokens.primary,
                contentColor = tokens.onPrimary,
              ),
            ) {
              Text(stringResource(R.string.prompt_send))
            }
          }
        }
      }
    }
  }
}