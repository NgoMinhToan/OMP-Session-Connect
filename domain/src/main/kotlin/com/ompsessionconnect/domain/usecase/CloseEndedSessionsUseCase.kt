package com.ompsessionconnect.domain.usecase

import com.ompsessionconnect.domain.model.SessionPhase
import com.ompsessionconnect.domain.repository.SessionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Closes every stored session whose room ended or failed. */
class CloseEndedSessionsUseCase @Inject constructor(
  private val repository: SessionRepository,
) {
  suspend operator fun invoke(): Result<Int> = runCatching {
    val ended = repository.observeSessions().first()
      .filter { it.snapshot?.phase == SessionPhase.ENDED || it.snapshot?.phase == SessionPhase.FAILED }
      .map { it.id }
      .toSet()
    if (ended.isEmpty()) return@runCatching 0
    repository.closeSessions(ended).getOrThrow()
    ended.size
  }
}
