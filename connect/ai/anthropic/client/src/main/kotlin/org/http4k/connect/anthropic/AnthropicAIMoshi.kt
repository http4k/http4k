package org.http4k.connect.anthropic

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Role
import org.http4k.connect.model.Timestamp
import org.http4k.connect.withAiMappings
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
        .value(Prompt)
        .value(Role)
        .value(ToolUseId)
        .value(Timestamp)
        .value(UserId)
        .value(ResponseId)
        .value(Base64Blob)
        .done()
)

@KotshiJsonAdapterFactory
object AnthropicAIJsonAdapterFactory : JsonAdapter.Factory by KotshiAnthropicAIJsonAdapterFactory
