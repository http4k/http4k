package org.http4k.ai.llm.decide

import org.http4k.ai.model.ModelName
import org.http4k.ai.model.TokenUsage

data class DecideResponse(val answers: Map<String, Answer>, val metadata: Metadata) {
    data class Metadata(val model: ModelName, val usage: TokenUsage? = null)
}
