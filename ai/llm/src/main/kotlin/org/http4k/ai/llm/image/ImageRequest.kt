package org.http4k.ai.llm.image

import org.http4k.ai.model.ModelName
import org.http4k.ai.model.UserPrompt
import org.http4k.connect.model.MimeType
import org.http4k.connect.model.MimeType.Companion.IMAGE_JPG

data class ImageRequest(
    val model: ModelName,
    val prompt: UserPrompt,
    val responseFormat: ImageResponseFormat,
    val size: Size? = null,
    val mimeType: MimeType = IMAGE_JPG,
    val quantity: Int = 1,
)
