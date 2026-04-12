package org.http4k.connect.amazon.apprunner

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.apprunner.model.NextToken
import org.http4k.connect.amazon.apprunner.model.ServiceId
import org.http4k.connect.amazon.apprunner.model.ServiceName
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object AppRunnerMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(AppRunnerJsonAdapterFactory)
        .value(NextToken)
        .value(ServiceId)
        .value(ServiceName)
        .done()
)

@KotshiJsonAdapterFactory
object AppRunnerJsonAdapterFactory : JsonAdapter.Factory by KotshiAppRunnerJsonAdapterFactory
