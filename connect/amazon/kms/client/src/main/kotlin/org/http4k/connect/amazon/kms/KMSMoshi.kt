package org.http4k.connect.amazon.kms

import com.squareup.moshi.JsonAdapter
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import se.ansman.kotshi.KotshiJsonAdapterFactory

object KMSMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(KMSJsonAdapterFactory)
        .done()
)

@KotshiJsonAdapterFactory
object KMSJsonAdapterFactory : JsonAdapter.Factory by KotshiKMSJsonAdapterFactory
