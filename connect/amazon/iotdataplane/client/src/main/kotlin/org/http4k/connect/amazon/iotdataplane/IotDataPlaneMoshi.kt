package org.http4k.connect.amazon.iotdataplane

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.iotdataplane.model.ShadowName
import org.http4k.connect.amazon.iotdataplane.model.ThingName
import org.http4k.connect.amazon.iotdataplane.model.TopicName
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object IotDataPlaneMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(IotDataPlaneJsonAdapterFactory)
        .value(ShadowName)
        .value(ThingName)
        .value(TopicName)
        .done()
)

@KotshiJsonAdapterFactory
object IotDataPlaneJsonAdapterFactory : JsonAdapter.Factory by KotshiIotDataPlaneJsonAdapterFactory
