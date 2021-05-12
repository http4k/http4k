package org.http4k.format

import com.squareup.moshi.Moshi

object AwsLambdaMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(adapter { DynamodbEventAdapter })
        .add(adapter { KinesisEventAdapter })
        .add(adapter { KinesisFirehoseEventAdapter })
        .add(adapter { S3EventAdapter })
        .add(adapter { ScheduledEventAdapter })
        .add(adapter { SNSEventAdapter })
        .add(adapter { SQSEventAdapter })
        .asConfigurable()
        .withStandardMappings()
        .done()
)
