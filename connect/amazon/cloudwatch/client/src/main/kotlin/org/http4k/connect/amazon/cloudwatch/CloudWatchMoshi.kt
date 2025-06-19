package org.http4k.connect.amazon.cloudwatch

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.cloudwatch.model.AlarmName
import org.http4k.connect.amazon.cloudwatch.model.ExtendedStatistic
import org.http4k.connect.amazon.cloudwatch.model.PercentileExtendedStatistic
import org.http4k.connect.amazon.cloudwatch.model.MetricName
import org.http4k.connect.amazon.cloudwatch.model.Namespace
import org.http4k.connect.amazon.cloudwatch.model.NextToken
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object CloudWatchMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(CloudWatchJsonAdapterFactory)
        .value(AlarmName)
        .value(ExtendedStatistic)
        .value(MetricName)
        .value(PercentileExtendedStatistic)
        .value(Namespace)
        .value(NextToken)
        .done()
)

@KotshiJsonAdapterFactory
object CloudWatchJsonAdapterFactory : JsonAdapter.Factory by KotshiCloudWatchJsonAdapterFactory
