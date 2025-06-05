package org.http4k.ai.llm.chat

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.ai.llm.util.LLMJsonAdapterFactory
import org.http4k.ai.util.withAiMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object ChatJson : ConfigurableMoshi(
    Moshi.Builder()
        .add(LLMJsonAdapterFactory)
        .add(ChatJsonAdapterFactory)
        .add(MapAdapter)
        .add(ListAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAiMappings()
        .done()
)

@KotshiJsonAdapterFactory
object ChatJsonAdapterFactory : JsonAdapter.Factory by KotshiChatJsonAdapterFactory

