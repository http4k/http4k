package org.http4k.connect.amazon.xray

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.xray.model.SegmentId
import org.http4k.connect.amazon.xray.model.TraceId
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object XRayMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(XRayJsonAdapterFactory)
        .value(AwsAccount)
        .value(SegmentId)
        .value(TraceId)
        .done()
)

@KotshiJsonAdapterFactory
object XRayJsonAdapterFactory : JsonAdapter.Factory by KotshiXRayJsonAdapterFactory
