package org.http4k.format

import com.squareup.moshi.Moshi

object AwsLambdaMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .addTyped(DynamodbEventAdapter)
        .addTyped(KinesisEventAdapter)
        .addTyped(KinesisFirehoseEventAdapter)
        .addTyped(S3EventAdapter)
        .addTyped(ScheduledEventAdapter)
        .addTyped(SNSEventAdapter)
        .addTyped(SQSEventAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)
