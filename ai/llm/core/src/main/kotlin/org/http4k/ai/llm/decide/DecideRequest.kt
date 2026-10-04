package org.http4k.ai.llm.decide

import org.http4k.ai.model.ModelName

data class DecideRequest(val state: String, val questions: Map<String, Question>, val model: ModelName)
