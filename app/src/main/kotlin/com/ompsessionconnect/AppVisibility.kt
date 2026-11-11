package com.ompsessionconnect

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Process-wide foreground flag; the service suppresses notifications while true. */
@Singleton
class AppVisibility @Inject constructor() {
  private val _visible = MutableStateFlow(false)
  val visible: StateFlow<Boolean> = _visible.asStateFlow()

  fun setVisible(value: Boolean) {
    _visible.value = value
  }
}
