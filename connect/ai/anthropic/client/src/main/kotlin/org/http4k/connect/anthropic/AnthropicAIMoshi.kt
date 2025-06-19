package org.http4k.connect.anthropic

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.model.Timestamp
import org.http4k.ai.util.withAiMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object AnthropicAIMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(AnthropicAIJsonAdapterFactory)
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAiMappings()
        .value(ModelType)
        .value(ToolUseId)
        .value(Timestamp)
        .value(UserId)
        .done()
)

@KotshiJsonAdapterFactory
object AnthropicAIJsonAdapterFactory : JsonAdapter.Factory by KotshiAnthropicAIJsonAdapterFactory
