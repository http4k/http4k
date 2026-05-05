package org.http4k.format

import com.squareup.moshi.Moshi

object AwsLambdaMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(CloudWatchLogsEventAdapter)
        .add(DynamodbEventAdapter)
        .add(EventBridgeEventAdapter)
        .add(KinesisEventAdapter)
        .add(KinesisFirehoseEventAdapter)
        .add(S3EventAdapter)
        .add(ScheduledEventAdapter)
        .add(SNSEventAdapter)
        .add(SQSBatchResponseAdapter)
        .add(SQSEventAdapter)
        .addLast(MapAdapter)
        .addLast(ListAdapter)
        .addLast(EventAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)
