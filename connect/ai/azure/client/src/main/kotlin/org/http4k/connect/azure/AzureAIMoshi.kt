package org.http4k.connect.azure

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.ModelName
import org.http4k.connect.model.Role
import org.http4k.connect.model.Timestamp
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
        .value(Base64Blob)
        .value(CompletionId)
        .value(ModelName)
        .value(ModelProvider)
        .value(ModelType)
        .value(ObjectType)
        .value(ObjectId)
        .value(Prompt)
        .value(Role)
        .value(Timestamp)
        .value(TokenId)
        .value(User)
        .done()
)

@KotshiJsonAdapterFactory
object AzureAIJsonAdapterFactory : JsonAdapter.Factory by KotshiAzureAIJsonAdapterFactory
