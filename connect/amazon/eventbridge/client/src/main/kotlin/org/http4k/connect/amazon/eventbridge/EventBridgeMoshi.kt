package org.http4k.connect.amazon.eventbridge

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.model.EndpointId
import org.http4k.connect.amazon.model.EventBusName
import org.http4k.connect.amazon.model.EventDetail
import org.http4k.connect.amazon.model.EventDetailType
import org.http4k.connect.amazon.model.EventId
import org.http4k.connect.amazon.model.EventSource
import org.http4k.connect.amazon.model.EventSourceName
import org.http4k.connect.amazon.model.Policy
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object EventBridgeMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(EventBridgeJsonAdapterFactory)
        .value(EndpointId)
        .value(EventBusName)
        .value(EventDetailType)
        .value(EventDetail)
        .value(EventId)
        .value(EventSource)
        .value(EventSourceName)
        .value(Policy)
        .done()
)

@KotshiJsonAdapterFactory
object EventBridgeJsonAdapterFactory : JsonAdapter.Factory by KotshiEventBridgeJsonAdapterFactory
