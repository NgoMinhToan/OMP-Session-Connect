package com.ompsessionconnect.domain.usecase

import com.ompsessionconnect.domain.model.AgentTranscriptPage
import com.ompsessionconnect.domain.repository.SessionRepository
import javax.inject.Inject

class FetchAgentTranscriptUseCase @Inject constructor(
  private val repository: SessionRepository,
) {
  suspend operator fun invoke(
    sessionId: String,
    agentId: String,
    fromOffset: Long,
  ): Result<AgentTranscriptPage> = repository.fetchAgentTranscript(sessionId, agentId, fromOffset)
}
