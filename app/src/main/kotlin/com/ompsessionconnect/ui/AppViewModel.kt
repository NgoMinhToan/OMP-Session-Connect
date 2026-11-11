package com.ompsessionconnect.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.model.SessionSummary
import com.ompsessionconnect.domain.usecase.AbortSessionUseCase
import com.ompsessionconnect.domain.usecase.AddSessionUseCase
import com.ompsessionconnect.domain.usecase.CloseEndedSessionsUseCase
import com.ompsessionconnect.domain.usecase.CloseSessionUseCase
import com.ompsessionconnect.domain.usecase.ObserveSessionsUseCase
import com.ompsessionconnect.domain.usecase.ReconnectSessionUseCase
import com.ompsessionconnect.domain.usecase.RespondToUiUseCase
import com.ompsessionconnect.domain.usecase.SendCommandUseCase
import com.ompsessionconnect.domain.usecase.SendPromptUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppEvent {
  data class Error(val message: String?) : AppEvent
  data object SessionClosed : AppEvent
  data class ClosedEnded(val count: Int) : AppEvent
}

@HiltViewModel
class AppViewModel @Inject constructor(
  observeSessions: ObserveSessionsUseCase,
  private val addSession: AddSessionUseCase,
  private val closeSession: CloseSessionUseCase,
  private val closeEndedSessions: CloseEndedSessionsUseCase,
  private val sendPromptUseCase: SendPromptUseCase,
  private val sendCommandUseCase: SendCommandUseCase,
  private val abortSession: AbortSessionUseCase,
  private val respondToUiUseCase: RespondToUiUseCase,
  private val reconnectSession: ReconnectSessionUseCase,
) : ViewModel() {

  /** null until the first store emission so the UI can show a loader. */
  val sessions: StateFlow<List<SessionOverview>?> = observeSessions()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

  private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
  val events: SharedFlow<AppEvent> = _events.asSharedFlow()

  suspend fun add(link: String): Result<SessionSummary> = addSession(link)

  fun close(sessionId: String) {
    viewModelScope.launch {
      closeSession(sessionId)
        .onSuccess { _events.emit(AppEvent.SessionClosed) }
        .onFailure { _events.emit(AppEvent.Error(it.message)) }
    }
  }

  fun closeEnded() {
    viewModelScope.launch {
      closeEndedSessions()
        .onSuccess { count -> _events.emit(AppEvent.ClosedEnded(count)) }
        .onFailure { _events.emit(AppEvent.Error(it.message)) }
    }
  }

  fun sendPrompt(sessionId: String, text: String) {
    viewModelScope.launch {
      sendPromptUseCase(sessionId, text).onFailure {
        _events.emit(AppEvent.Error(it.message))
      }
    }
  }

  fun sendCommand(sessionId: String, text: String) {
    viewModelScope.launch {
      sendCommandUseCase(sessionId, text).onFailure {
        _events.emit(AppEvent.Error(it.message))
      }
    }
  }

  fun abort(sessionId: String) {
    viewModelScope.launch { abortSession(sessionId) }
  }

  fun respondToUi(sessionId: String, requestId: Int, value: String?) {
    viewModelScope.launch { respondToUiUseCase(sessionId, requestId, value) }
  }

  fun reconnect(sessionId: String) {
    viewModelScope.launch { reconnectSession(sessionId) }
  }
}
