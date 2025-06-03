package org.http4k.ai.chat

import org.http4k.ai.util.AiJsonNode

data class ResponseFormat(val type: ResponseFormatType, val jsonSchema: AiJsonNode? = null)
