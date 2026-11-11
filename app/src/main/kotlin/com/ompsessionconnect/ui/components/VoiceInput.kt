package com.ompsessionconnect.ui.components

import android.content.Context
import android.content.Intent
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Thin wrapper around Android's on-device/native speech recognizer.
 *
 * Partial results replace the draft (prefixed with whatever the user had
 * typed when the microphone was tapped), so dictation does not duplicate text.
 */
class VoiceInputController(context: Context) {
  var listening by mutableStateOf(false)
    private set
  var rms by mutableFloatStateOf(0f)
    private set
  var errorCode by mutableIntStateOf(0)
    private set

  val available: Boolean = runCatching {
    SpeechRecognizer.isRecognitionAvailable(context)
  }.getOrDefault(false)

  var onTextCallback: (String) -> Unit = {}
  var onErrorCallback: (Int) -> Unit = {}

  private val appContext = context.applicationContext
  private var baseText = ""
  private val recognizer: SpeechRecognizer? = if (available) {
    runCatching { SpeechRecognizer.createSpeechRecognizer(appContext) }.getOrNull()
  } else {
    null
  }

  private val listener = object : RecognitionListener {
    override fun onReadyForSpeech(params: Bundle?) {
      listening = true
      errorCode = 0
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) {
      rms = ((rmsdB.coerceIn(-2f, 10f) + 2f) / 12f).coerceIn(0f, 1f)
    }

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
      listening = false
    }

    override fun onError(error: Int) {
      listening = false
      errorCode = error
      if (error != SpeechRecognizer.ERROR_CLIENT) onErrorCallback(error)
    }

    override fun onResults(results: Bundle?) {
      listening = false
      val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        ?.takeIf { it.isNotBlank() }
      if (text != null) onTextCallback(join(baseText, text))
    }

    override fun onPartialResults(partialResults: Bundle?) {
      val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: return
      listening = true
      onTextCallback(join(baseText, text))
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
  }

  init {
    recognizer?.setRecognitionListener(listener)
  }

  fun start(base: String) {
    val speech = recognizer ?: return
    baseText = base.trimEnd()
    errorCode = 0
    listening = true
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
      putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
      putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
    }
    runCatching { speech.startListening(intent) }
      .onFailure {
        listening = false
        errorCode = SpeechRecognizer.ERROR_CLIENT
      }
  }

  fun stop() {
    listening = false
    rms = 0f
    runCatching { recognizer?.stopListening() }
  }

  fun destroy() {
    listening = false
    runCatching { recognizer?.cancel() }
    runCatching { recognizer?.destroy() }
  }

  private fun join(base: String, addition: String): String =
    if (base.isBlank()) addition else "$base $addition"
}

@Composable
fun rememberVoiceInput(
  onText: (String) -> Unit,
  onError: (Int) -> Unit,
): VoiceInputController {
  val context = LocalContext.current
  val latestText by rememberUpdatedState(onText)
  val latestError by rememberUpdatedState(onError)
  val controller = remember(context) {
    VoiceInputController(context).apply {
      this.onTextCallback = { latestText(it) }
      this.onErrorCallback = { latestError(it) }
    }
  }
  DisposableEffect(controller) {
    onDispose { controller.destroy() }
  }
  return controller
}
