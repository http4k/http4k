package org.http4k.ai.llm.image

import org.http4k.ai.llm.LLMResult

fun interface ImageGeneration {
    operator fun invoke(request: ImageRequest): LLMResult<ImageResponse>

    companion object
}
