package com.ompsessionconnect.domain.usecase

import com.ompsessionconnect.domain.repository.SessionRepository
import javax.inject.Inject

class ReconnectSessionUseCase @Inject constructor(
  private val repository: SessionRepository,
) {
  suspend operator fun invoke(sessionId: String) = repository.reconnect(sessionId)
}
