package org.http4k.connect.amazon.scheduler

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.scheduler.model.ClientToken
import org.http4k.connect.amazon.scheduler.model.ScheduleExpression
import org.http4k.connect.amazon.scheduler.model.ScheduleGroupName
import org.http4k.connect.amazon.scheduler.model.ScheduleName
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object SchedulerMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(SchedulerJsonAdapterFactory)
        .value(ScheduleName)
        .value(ScheduleGroupName)
        .value(ClientToken)
        .value(ScheduleExpression)
        .done()
)

@KotshiJsonAdapterFactory
object SchedulerJsonAdapterFactory : JsonAdapter.Factory by KotshiSchedulerJsonAdapterFactory
