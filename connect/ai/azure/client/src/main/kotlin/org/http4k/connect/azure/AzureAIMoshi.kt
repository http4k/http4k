package org.http4k.connect.azure

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.Timestamp
import org.http4k.connect.withAiMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object AzureAIMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(AzureAIJsonAdapterFactory)
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAiMappings()
        .value(Base64Blob)
        .value(CompletionId)
        .value(ModelProvider)
        .value(ModelType)
        .value(ObjectType)
        .value(ObjectId)
        .value(Prompt)
        .value(Timestamp)
        .value(TokenId)
        .value(User)
        .done()
)

@KotshiJsonAdapterFactory
object AzureAIJsonAdapterFactory : JsonAdapter.Factory by KotshiAzureAIJsonAdapterFactory
