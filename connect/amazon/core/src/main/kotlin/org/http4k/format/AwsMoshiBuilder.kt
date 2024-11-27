package org.http4k.format

import com.squareup.moshi.JsonAdapter.Factory
import com.squareup.moshi.Moshi

fun AwsMoshiBuilder(factory: Factory) =
    Moshi.Builder()
        .add(factory)
        .add(AwsCoreJsonAdapterFactory())
        .add(ListAdapter)
        .add(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
