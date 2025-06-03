package org.http4k.ai.image

import org.http4k.ai.AiResult
import org.http4k.ai.model.Resource
import org.http4k.ai.model.Prompt

typealias ImageModel = (ImageRequest) -> AiResult<ImageResponse>

data class ImageRequest(val prompt: Prompt)
data class ImageResponse(val resource: Resource)
