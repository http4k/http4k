package org.http4k.connect.amazon.firehose

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.model.DeliveryStreamName
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object FirehoseMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(FirehoseJsonAdapterFactory)
        .value(DeliveryStreamName)
        .done()
)

@KotshiJsonAdapterFactory
object FirehoseJsonAdapterFactory : JsonAdapter.Factory by KotshiFirehoseJsonAdapterFactory
