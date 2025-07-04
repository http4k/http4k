package org.http4k.ai.llm

import org.http4k.connect.openai.OpenAI

/**
 * Provides an OpenAI compatible API client. Since the OpenAI API is used across
 * many model providers, this allows you to plug in any compatible client
 */
fun interface OpenAICompatibleClient : () -> OpenAI
