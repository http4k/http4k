package org.http4k.format

import com.squareup.moshi.Moshi

object AwsLambdaMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .addTyped(CloudWatchLogsEventAdapter)
        .addTyped(DynamodbEventAdapter)
        .addTyped(KinesisEventAdapter)
        .addTyped(KinesisFirehoseEventAdapter)
        .addTyped(S3EventAdapter)
        .addTyped(ScheduledEventAdapter)
        .addTyped(SNSEventAdapter)
        .addTyped(SQSEventAdapter)
        .addLast(MapAdapter)
        .addLast(ListAdapter)
        .addLast(EventAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)
