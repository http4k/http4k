package org.http4k.ai.llm.decide

import org.http4k.ai.llm.model.Content
import org.http4k.ai.model.ModelName

data class DecideRequest(val state: List<Content>, val questions: Map<String, Question>, val model: ModelName) {
    constructor(state: String, questions: Map<String, Question>, model: ModelName) :
        this(listOf(Content.Text(state)), questions, model)
}
