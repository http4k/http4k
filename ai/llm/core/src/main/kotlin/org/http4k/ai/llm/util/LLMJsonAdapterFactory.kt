package org.http4k.ai.llm.util

import com.squareup.moshi.JsonAdapter
import se.ansman.kotshi.KotshiJsonAdapterFactory

@KotshiJsonAdapterFactory
object LLMJsonAdapterFactory : JsonAdapter.Factory by KotshiLLMJsonAdapterFactory
