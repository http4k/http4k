package org.http4k.connect.amazon.eventbridge

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.model.EndpointId
import org.http4k.connect.amazon.model.EventBusName
import org.http4k.connect.amazon.model.EventDetail
import org.http4k.connect.amazon.model.EventDetailType
import org.http4k.connect.amazon.model.EventId
import org.http4k.connect.amazon.model.EventSource
import org.http4k.connect.amazon.model.EventSourceName
import org.http4k.connect.amazon.model.Policy
import org.http4k.format.AwsCoreJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object EventBridgeMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(EventBridgeJsonAdapterFactory)
        .add(AwsCoreJsonAdapterFactory())
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
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
