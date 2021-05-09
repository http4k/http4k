package org.http4k.format

import com.squareup.moshi.Moshi

object AwsLambdaMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(ScheduledEventAdapter)
        .add(SQSEventAdapter)
        .addLast(CollectionEdgeCasesAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)
