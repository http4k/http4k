package org.http4k.connect.amazon.iot

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.iot.model.JobId
import org.http4k.connect.amazon.iot.model.StreamId
import org.http4k.connect.amazon.iot.model.ThingName
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object IotMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(IotJsonAdapterFactory)
        .value(JobId)
        .value(StreamId)
        .value(ThingName)
        .done()
)

@KotshiJsonAdapterFactory
object IotJsonAdapterFactory : JsonAdapter.Factory by KotshiIotJsonAdapterFactory
