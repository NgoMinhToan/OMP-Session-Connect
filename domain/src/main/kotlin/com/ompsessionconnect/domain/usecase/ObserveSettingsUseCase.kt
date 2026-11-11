package com.ompsessionconnect.domain.usecase

import com.ompsessionconnect.domain.model.AppSettings
import com.ompsessionconnect.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
  private val repository: SettingsRepository,
) {
  operator fun invoke(): Flow<AppSettings> = repository.settings
}
