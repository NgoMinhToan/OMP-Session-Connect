package com.ompsessionconnect.domain.usecase

import com.ompsessionconnect.domain.repository.SessionRepository
import javax.inject.Inject

/** Answers a host `ask`/editor request; null cancels the request. */
class RespondToUiUseCase @Inject constructor(
  private val repository: SessionRepository,
) {
  suspend operator fun invoke(sessionId: String, requestId: Int, value: String?) =
    repository.respondToUi(sessionId, requestId, value)
}
