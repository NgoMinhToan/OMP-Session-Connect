package com.ompsessionconnect.domain.usecase

import com.ompsessionconnect.domain.model.SessionOverview
import com.ompsessionconnect.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSessionUseCase @Inject constructor(
  private val repository: SessionRepository,
) {
  operator fun invoke(sessionId: String): Flow<SessionOverview?> = repository.observeSession(sessionId)
}
