package org.http4k.connect.openai

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.ai.util.withAiMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.MoshiNodeAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object OpenAIMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(OpenAIJsonAdapterFactory)
        .add(MoshiNodeAdapter)
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAiMappings()
        .value(OpenAIOrg)
        .value(ObjectType)
        .value(ObjectId)
        .value(TokenId)
        .value(User)
        .done()
)

@KotshiJsonAdapterFactory
object OpenAIJsonAdapterFactory : JsonAdapter.Factory by KotshiOpenAIJsonAdapterFactory
