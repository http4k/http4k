package org.http4k.connect.slack

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.slack.model.ChannelId
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object SlackMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(SlackJsonAdapterFactory)
        .add(MapAdapter)
        .add(ListAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withSlackMappings()
        .done()
)

@KotshiJsonAdapterFactory
object SlackJsonAdapterFactory : JsonAdapter.Factory by KotshiSlackJsonAdapterFactory

fun <T> AutoMappingConfiguration<T>.withSlackMappings() = apply {
    value(ChannelId)
}

