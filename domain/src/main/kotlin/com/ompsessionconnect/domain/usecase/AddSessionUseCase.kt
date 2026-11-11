package com.ompsessionconnect.domain.usecase

import com.ompsessionconnect.domain.model.SessionSummary
import com.ompsessionconnect.domain.repository.SessionRepository
import javax.inject.Inject

class AddSessionUseCase @Inject constructor(
  private val repository: SessionRepository,
) {
  suspend operator fun invoke(link: String): Result<SessionSummary> = repository.addSession(link)
}
