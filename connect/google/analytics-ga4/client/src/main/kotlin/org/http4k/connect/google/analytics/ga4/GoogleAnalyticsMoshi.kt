package org.http4k.connect.google.analytics.ga4

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.google.analytics.model.ClientId
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object GoogleAnalyticsMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(GoogleAnalyticsJsonAdapterFactory)
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .value(ClientId)
        .withStandardMappings()
        .done()
)

@KotshiJsonAdapterFactory
object GoogleAnalyticsJsonAdapterFactory : JsonAdapter.Factory by KotshiGoogleAnalyticsJsonAdapterFactory
