package org.http4k.format

import com.squareup.moshi.Moshi

object AwsLambdaMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(CollectionEdgeCasesAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)
