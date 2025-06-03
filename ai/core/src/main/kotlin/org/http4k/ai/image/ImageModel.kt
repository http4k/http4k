package org.http4k.ai.image

import org.http4k.ai.AiResult
import org.http4k.ai.model.Resource
import org.http4k.ai.model.UserPrompt

typealias ImageModel = (ImageRequest) -> AiResult<ImageResponse>

data class ImageRequest(val prompt: UserPrompt)
data class ImageResponse(val resource: Resource)
