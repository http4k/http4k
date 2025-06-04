package org.http4k.ai.llm.model

import org.http4k.ai.llm.util.LLMJsonNode

data class ResponseFormat(val type: ResponseFormatType, val schema: LLMJsonNode? = null)
