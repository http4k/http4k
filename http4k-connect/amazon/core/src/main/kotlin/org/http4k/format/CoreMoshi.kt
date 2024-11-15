package org.http4k.format

import com.squareup.moshi.Moshi

object AwsCoreMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(AwsCoreJsonAdapterFactory())
        .add(CoreAdapterFactory)
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
        .done()
)
