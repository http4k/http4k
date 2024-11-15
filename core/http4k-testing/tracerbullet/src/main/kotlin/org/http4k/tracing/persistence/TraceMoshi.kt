package org.http4k.tracing.persistence

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import org.http4k.tracing.BiDirectional
import org.http4k.tracing.FireAndForget
import org.http4k.tracing.RequestResponse
import org.http4k.tracing.Trace

object TraceMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .addLast(ListAdapter)
        .add(
            PolymorphicJsonAdapterFactory
                .of(Trace::class.java, "type")
                .withSubtype(RequestResponse::class.java, "RequestResponse")
                .withSubtype(BiDirectional::class.java, "BiDirectional")
                .withSubtype(FireAndForget::class.java, "FireAndForget")
        )
        .asConfigurable()
        .withStandardMappings()
        .done()
)