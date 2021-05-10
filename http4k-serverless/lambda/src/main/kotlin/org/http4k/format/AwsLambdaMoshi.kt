package org.http4k.format

import com.squareup.moshi.Moshi

object AwsLambdaMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(DynamodbEventAdapter)
        .add(KinesisEventAdapter)
        .add(KinesisFirehoseEventAdapter)
        .add(S3EventAdapter)
        .add(ScheduledEventAdapter)
        .add(SNSEventAdapter)
        .add(SQSEventAdapter)
        .addLast(CollectionEdgeCasesAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)
