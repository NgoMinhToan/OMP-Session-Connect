package com.ompsessionconnect.domain.usecase

import com.ompsessionconnect.domain.repository.SessionRepository
import javax.inject.Inject

class SendPromptUseCase @Inject constructor(
  private val repository: SessionRepository,
) {
  suspend operator fun invoke(sessionId: String, text: String): Result<Unit> {
    if (text.isBlank()) return Result.success(Unit)
    return repository.sendPrompt(sessionId, text.trim())
  }
}
