package org.http4k.connect.amazon.apprunner

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.apprunner.model.NextToken
import org.http4k.connect.amazon.apprunner.model.ServiceId
import org.http4k.connect.amazon.apprunner.model.ServiceName
import org.http4k.format.AwsCoreJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object AppRunnerMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(AppRunnerJsonAdapterFactory)
        .add(AwsCoreJsonAdapterFactory())
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
        .value(NextToken)
        .value(ServiceId)
        .value(ServiceName)
        .done()
)

@KotshiJsonAdapterFactory
object AppRunnerJsonAdapterFactory : JsonAdapter.Factory by KotshiAppRunnerJsonAdapterFactory
