package org.http4k.ai.llm.util

import com.squareup.moshi.Moshi
import org.http4k.ai.util.withAiMappings
import org.http4k.format.ArrayItemsJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.EventAdapter
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.MoshiNodeAdapter
import org.http4k.format.ProtocolStatusAdapter
import org.http4k.format.SchemaNodeJsonAdapterFactory
import org.http4k.format.SetAdapter
import org.http4k.format.ThrowableAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings

object LLMJson : ConfigurableMoshi(
    Moshi.Builder()
        .add(LLMJsonAdapterFactory)
        .addLast(EventAdapter)
        .addLast(ThrowableAdapter)
        .addLast(ListAdapter)
        .addLast(SetAdapter)
        .addLast(MapAdapter)
        .addLast(MoshiNodeAdapter)
        .addLast(ArrayItemsJsonAdapterFactory)
        .asConfigurable()
        .withStandardMappings()
        .withAiMappings()
        .done()
)
