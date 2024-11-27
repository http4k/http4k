package org.http4k.connect.amazon.cloudwatchlogs

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.cloudwatchlogs.model.EventId
import org.http4k.connect.amazon.cloudwatchlogs.model.LogGroupName
import org.http4k.connect.amazon.cloudwatchlogs.model.LogIndex
import org.http4k.connect.amazon.cloudwatchlogs.model.LogStreamName
import org.http4k.connect.amazon.cloudwatchlogs.model.NextToken
import org.http4k.format.AwsCoreJsonAdapterFactory
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object CloudWatchLogsMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(CloudWatchLogsJsonAdapterFactory)
        .value(EventId)
        .value(NextToken)
        .value(LogGroupName)
        .value(LogIndex)
        .value(LogStreamName)
        .done()
)

@KotshiJsonAdapterFactory
object CloudWatchLogsJsonAdapterFactory : JsonAdapter.Factory by KotshiCloudWatchLogsJsonAdapterFactory
