package org.http4k.ai.llm.decide

import org.http4k.ai.llm.LLMResult

fun interface Decide {
    operator fun invoke(request: DecideRequest): LLMResult<DecideResponse>

    companion object
}
