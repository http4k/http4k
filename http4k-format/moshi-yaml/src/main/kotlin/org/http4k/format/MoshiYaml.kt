package org.http4k.format

import com.squareup.moshi.Moshi.Builder

object MoshiYaml : ConfigurableMoshiYaml(
    Builder()
        .addLast(EventAdapter)
        .addLast(ThrowableAdapter)
        .addLast(ListAdapter)
        .addLast(MapAdapter)
        .asConfigurable()
        .withStandardMappings()
        .done()
)
