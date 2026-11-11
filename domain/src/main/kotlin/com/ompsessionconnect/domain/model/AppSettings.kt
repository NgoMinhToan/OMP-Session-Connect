package com.ompsessionconnect.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val notificationsEnabled: Boolean = true,
  val notifyTurnFinished: Boolean = true,
  val notifyErrors: Boolean = true,
  val notifyLiveProgress: Boolean = true,
  val notifyLiveMessages: Boolean = true,
)
