package org.http4k.ai.llm.util

import com.squareup.moshi.Moshi
import org.http4k.ai.util.withAiMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings

object LLMJson : ConfigurableMoshi(
    Moshi.Builder()
        .add(LLMJsonAdapterFactory)
        .add(MapAdapter)
        .add(ListAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAiMappings()
        .done()
)
