package org.http4k.connect.amazon.iotjobsdataplane

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.iotjobsdataplane.model.JobId
import org.http4k.connect.amazon.iotjobsdataplane.model.ThingName
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object IotJobsDataPlaneMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(IotJobsDataPlaneJsonAdapterFactory)
        .value(JobId)
        .value(ThingName)
        .done()
)

@KotshiJsonAdapterFactory
object IotJobsDataPlaneJsonAdapterFactory : JsonAdapter.Factory by KotshiIotJobsDataPlaneJsonAdapterFactory
